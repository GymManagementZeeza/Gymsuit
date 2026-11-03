package com.zeezaglobal.gymmanagement.security;

import com.zeezaglobal.gymmanagement.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Fixed-window limits for login, OTP sending and OTP checks, shared across backend
 * replicas through Redis. Each action is counted per client IP and per email: the IP
 * limit stops one source spraying many accounts, the email limit stops many sources
 * targeting one account.
 *
 * <p>Every attempt counts, successful or not. A Redis outage lets requests through,
 * matching how the cache degrades in {@code RedisConfig}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthRateLimiter {

    @Getter
    public enum Action {
        LOGIN(10, 60, Duration.ofMinutes(15)),
        OTP_SEND(5, 30, Duration.ofHours(1)),
        OTP_VERIFY(5, 60, Duration.ofMinutes(10));

        private final int emailLimit;
        // Higher than the email limit: a gym's members can all share one Wi-Fi address.
        private final int ipLimit;
        private final Duration window;

        Action(int emailLimit, int ipLimit, Duration window) {
            this.emailLimit = emailLimit;
            this.ipLimit = ipLimit;
            this.window = window;
        }
    }

    // Atomic so a crash can't leave a counter without an expiry, which would block it forever.
    private static final RedisScript<List> HIT_SCRIPT = RedisScript.of("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return {count, redis.call('TTL', KEYS[1])}
            """, List.class);

    private final StringRedisTemplate redis;

    /** @throws TooManyRequestsException when the caller or the account is over the limit */
    public void check(Action action, String email, HttpServletRequest request) {
        // IP first, so a blocked source can't keep using up its target's email limit.
        hit(action, "ip", clientIp(request), action.getIpLimit());
        hit(action, "email", hash(email.trim().toLowerCase(Locale.ROOT)), action.getEmailLimit());
    }

    private void hit(Action action, String scope, String subject, int limit) {
        String key = "gymsuit:ratelimit:" + action.name().toLowerCase(Locale.ROOT) + ":" + scope + ":" + subject;

        List<?> result;
        try {
            result = redis.execute(HIT_SCRIPT, List.of(key), String.valueOf(action.getWindow().toSeconds()));
        } catch (DataAccessException e) {
            log.warn("Rate limit check failed for {}; allowing the request: {}", action, e.getMessage());
            return;
        }

        long count = ((Number) result.get(0)).longValue();
        if (count > limit) {
            long retryAfterSeconds = Math.max(((Number) result.get(1)).longValue(), 1);
            log.warn("Rate limit exceeded for {} by {}", action, scope);
            throw new TooManyRequestsException(
                    "Too many attempts. Please try again in " + toMinutesText(retryAfterSeconds) + ".",
                    retryAfterSeconds);
        }
    }

    private static String toMinutesText(long seconds) {
        long minutes = (seconds + 59) / 60;
        return minutes == 1 ? "1 minute" : minutes + " minutes";
    }

    /**
     * Public traffic arrives through the Cloudflare Tunnel, so nginx sees one internal
     * address for everyone and only Cloudflare's header identifies the real client.
     * Not X-Forwarded-For: its leading entries are whatever the client chose to send.
     */
    private static String clientIp(HttpServletRequest request) {
        String cloudflareIp = request.getHeader("CF-Connecting-IP");
        if (cloudflareIp != null && !cloudflareIp.isBlank()) {
            return cloudflareIp.trim();
        }
        String nginxIp = request.getHeader("X-Real-IP");
        if (nginxIp != null && !nginxIp.isBlank()) {
            return nginxIp.trim();
        }
        return request.getRemoteAddr();
    }

    // Keeps addresses out of Redis and caps the key length, since login emails have no size limit.
    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
