package com.zeezaglobal.gymmanagement.security;

import com.zeezaglobal.gymmanagement.exception.TooManyRequestsException;
import com.zeezaglobal.gymmanagement.security.AuthRateLimiter.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthRateLimiterTest {

    private static final String EMAIL = "member@example.com";

    @Mock
    private StringRedisTemplate redis;

    @InjectMocks
    private AuthRateLimiter rateLimiter;

    @SuppressWarnings("unchecked")
    private org.mockito.stubbing.OngoingStubbing<Object> whenHit() {
        return when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)));
    }

    @SuppressWarnings("unchecked")
    private List<String> hitKeys(int times) {
        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        verify(redis, times(times)).execute(any(RedisScript.class), keys.capture(), any(Object[].class));
        return keys.getAllValues().stream().map(k -> k.get(0)).toList();
    }

    @Test
    void allowsRequestsAtTheLimit() {
        whenHit().thenReturn(List.of(60L, 100L)).thenReturn(List.of(10L, 100L));

        assertDoesNotThrow(() -> rateLimiter.check(Action.LOGIN, EMAIL, new MockHttpServletRequest()));
    }

    @Test
    void blocksTheEmailOverItsLimit() {
        whenHit().thenReturn(List.of(1L, 900L)).thenReturn(List.of(11L, 90L));

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> rateLimiter.check(Action.LOGIN, EMAIL, new MockHttpServletRequest()));

        assertEquals(90, ex.getRetryAfterSeconds());
        assertEquals("Too many attempts. Please try again in 2 minutes.", ex.getMessage());
    }

    @Test
    void blockedIpDoesNotUseUpTheEmailLimit() {
        whenHit().thenReturn(List.of(61L, 60L));

        assertThrows(TooManyRequestsException.class,
                () -> rateLimiter.check(Action.LOGIN, EMAIL, new MockHttpServletRequest()));

        assertTrue(hitKeys(1).get(0).contains(":ip:"));
    }

    @Test
    void allowsRequestsWhenRedisIsDown() {
        whenHit().thenThrow(new QueryTimeoutException("Redis command timed out"));

        assertDoesNotThrow(() -> rateLimiter.check(Action.OTP_SEND, EMAIL, new MockHttpServletRequest()));
    }

    @Test
    void usesTheCloudflareClientIpOverProxyAddresses() {
        whenHit().thenReturn(List.of(1L, 100L));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.1");
        request.addHeader("X-Real-IP", "172.18.0.1");
        request.addHeader("X-Forwarded-For", "6.6.6.6");
        request.addHeader("CF-Connecting-IP", "203.0.113.7");

        rateLimiter.check(Action.OTP_VERIFY, EMAIL, request);

        assertEquals("gymsuit:ratelimit:otp_verify:ip:203.0.113.7", hitKeys(2).get(0));
    }

    @Test
    void countsEmailsCaseInsensitivelyAndNeverStoresThemInKeys() {
        whenHit().thenReturn(List.of(1L, 100L));

        rateLimiter.check(Action.LOGIN, EMAIL, new MockHttpServletRequest());
        rateLimiter.check(Action.LOGIN, "  Member@Example.COM ", new MockHttpServletRequest());

        List<String> keys = hitKeys(4);
        assertEquals(keys.get(1), keys.get(3));
        assertFalse(keys.get(1).contains("example"));
    }
}
