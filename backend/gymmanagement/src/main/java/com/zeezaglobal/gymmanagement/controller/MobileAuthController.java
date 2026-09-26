package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.MobileAuthResponse;
import com.zeezaglobal.gymmanagement.dto.MobileForgotPasswordRequest;
import com.zeezaglobal.gymmanagement.dto.MobileLoginRequest;
import com.zeezaglobal.gymmanagement.dto.MobileRegisterRequest;
import com.zeezaglobal.gymmanagement.dto.MobileRegisterWithOtpRequest;
import com.zeezaglobal.gymmanagement.dto.MobileResetPasswordRequest;
import com.zeezaglobal.gymmanagement.dto.MobileSendOtpRequest;
import com.zeezaglobal.gymmanagement.dto.MobileVerifyOtpLoginRequest;
import com.zeezaglobal.gymmanagement.dto.RefreshRequest;
import com.zeezaglobal.gymmanagement.service.MobileAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mobile/auth")
@RequiredArgsConstructor
public class MobileAuthController {

    private final MobileAuthService mobileAuthService;

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(@Valid @RequestBody MobileSendOtpRequest request) {
        mobileAuthService.sendOtp(request);
        return ResponseEntity.ok(Map.of("message", "A 6-digit verification code has been sent to your email."));
    }

    @PostMapping("/verify-login")
    public ResponseEntity<MobileAuthResponse> verifyOtpLogin(@Valid @RequestBody MobileVerifyOtpLoginRequest request) {
        MobileAuthResponse response = mobileAuthService.verifyOtpLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register-otp")
    public ResponseEntity<MobileAuthResponse> registerWithOtp(@Valid @RequestBody MobileRegisterWithOtpRequest request) {
        MobileAuthResponse response = mobileAuthService.registerWithOtp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<MobileAuthResponse> login(@Valid @RequestBody MobileLoginRequest request) {
        MobileAuthResponse response = mobileAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<MobileAuthResponse> register(@Valid @RequestBody MobileRegisterRequest request) {
        MobileAuthResponse response = mobileAuthService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody MobileForgotPasswordRequest request) {
        mobileAuthService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "If an account exists for this email, a verification code has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody MobileResetPasswordRequest request) {
        mobileAuthService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password has been successfully updated. You can now log in."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<MobileAuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        MobileAuthResponse response = mobileAuthService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}
