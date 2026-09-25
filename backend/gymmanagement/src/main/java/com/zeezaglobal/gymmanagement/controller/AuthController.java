package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.EmailCheckResponse;
import com.zeezaglobal.gymmanagement.dto.EmailRequest;
import com.zeezaglobal.gymmanagement.dto.GoogleLoginRequest;
import com.zeezaglobal.gymmanagement.dto.LoginRequest;
import com.zeezaglobal.gymmanagement.dto.LoginResponse;
import com.zeezaglobal.gymmanagement.dto.OtpVerifyRequest;
import com.zeezaglobal.gymmanagement.dto.RefreshRequest;
import com.zeezaglobal.gymmanagement.security.RefreshCookieFactory;
import com.zeezaglobal.gymmanagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return withRefreshCookie(authService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return withRefreshCookie(authService.loginWithGoogle(request));
    }

    @PostMapping("/email/check")
    public EmailCheckResponse checkEmail(@Valid @RequestBody EmailRequest request) {
        return new EmailCheckResponse(authService.emailExists(request.email()));
    }

    @PostMapping("/otp/request")
    public ResponseEntity<Void> requestOtp(@Valid @RequestBody EmailRequest request) {
        authService.requestOtp(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<LoginResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return withRefreshCookie(authService.verifyOtp(request.email(), request.otp()));
    }

    /**
     * Silent session renewal. Accepts the refresh token from the httpOnly
     * cookie (web) or the JSON body (native clients). Rotates the refresh
     * token on every call.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String cookieToken,
            @RequestBody(required = false) RefreshRequest body) {
        String rawToken = cookieToken != null ? cookieToken
                : (body != null ? body.refreshToken() : null);
        return withRefreshCookie(authService.refreshSession(rawToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String cookieToken,
            @RequestBody(required = false) RefreshRequest body) {
        String rawToken = cookieToken != null ? cookieToken
                : (body != null ? body.refreshToken() : null);
        authService.logout(rawToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.clear().toString())
                .build();
    }

    private ResponseEntity<LoginResponse> withRefreshCookie(LoginResponse response) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.issue(response.refreshToken()).toString())
                .body(response);
    }
}
