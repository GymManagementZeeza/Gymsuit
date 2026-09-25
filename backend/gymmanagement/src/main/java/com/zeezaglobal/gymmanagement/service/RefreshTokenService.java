package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.entity.RefreshToken;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.repository.RefreshTokenRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Issues and validates opaque refresh tokens.
 *
 * <p>Refresh tokens are random 256-bit values. Only their SHA-256 hash is
 * persisted. Every successful refresh rotates the token: the presented token
 * is revoked and a fresh one is issued, so a stolen token is usable at most
 * once before the legitimate client's next refresh invalidates it.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.jwt.refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public record RefreshedSession(UserPrincipal principal, String refreshToken) {
    }

    @Transactional
    public String createToken(UserPrincipal principal) {
        String rawToken = generateRawToken();
        RefreshToken record = new RefreshToken();
        record.setTokenHash(sha256Hex(rawToken));
        record.setUserId(principal.getUserId());
        record.setIssuedAt(Instant.now());
        record.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        record.setRevoked(false);
        refreshTokenRepository.save(record);
        return rawToken;
    }

    /**
     * Validates the presented refresh token, rotates it, and returns the
     * principal plus the replacement raw token.
     */
    @Transactional
    public RefreshedSession rotate(String rawToken) {
        RefreshToken record = refreshTokenRepository.findByTokenHash(sha256Hex(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (record.isRevoked() || record.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token expired");
        }

        User user = userRepository.findById(record.getUserId())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (!user.isEnabled()) {
            throw new BadRequestException("This account has been disabled");
        }

        // Rotation: the presented token is single-use from here on.
        record.setRevoked(true);
        refreshTokenRepository.save(record);

        UserPrincipal principal = new UserPrincipal(user);
        return new RefreshedSession(principal, createToken(principal));
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(sha256Hex(rawToken)).ifPresent(record -> {
            record.setRevoked(true);
            refreshTokenRepository.save(record);
        });
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
