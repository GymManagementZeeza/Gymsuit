package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.EmailCheckResponse;
import com.zeezaglobal.gymmanagement.dto.EmailRequest;
import com.zeezaglobal.gymmanagement.dto.GoogleLoginRequest;
import com.zeezaglobal.gymmanagement.dto.LoginRequest;
import com.zeezaglobal.gymmanagement.dto.LoginResponse;
import com.zeezaglobal.gymmanagement.dto.OtpVerifyRequest;
import com.zeezaglobal.gymmanagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/google")
    public LoginResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request);
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
    public LoginResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return authService.verifyOtp(request.email(), request.otp());
    }
}
