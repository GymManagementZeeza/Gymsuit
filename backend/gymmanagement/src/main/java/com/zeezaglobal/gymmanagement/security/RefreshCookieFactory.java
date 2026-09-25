package com.zeezaglobal.gymmanagement.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds the httpOnly refresh-token cookie shared by every endpoint that
 * issues a session. The cookie is Secure + SameSite=None so the web client
 * (served from a different origin than the API) can use it, and scoped to
 * /api/auth so it is only sent where it is needed.
 */
@Component
public class RefreshCookieFactory {

    public static final String COOKIE_NAME = "gymsuit_refresh";

    private final long refreshExpirationMs;

    public RefreshCookieFactory(
            @Value("${app.jwt.refresh-expiration-ms:2592000000}") long refreshExpirationMs) {
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public ResponseCookie issue(String rawToken) {
        return ResponseCookie.from(COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/auth")
                .maxAge(Duration.ofMillis(refreshExpirationMs))
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
