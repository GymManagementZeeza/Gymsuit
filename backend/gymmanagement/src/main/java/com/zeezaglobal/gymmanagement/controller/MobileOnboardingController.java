package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.MobileOnboardingResponse;
import com.zeezaglobal.gymmanagement.dto.MobileOnboardingSyncRequest;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.service.MobileOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/onboarding")
@RequiredArgsConstructor
public class MobileOnboardingController {

    private final MobileOnboardingService mobileOnboardingService;
    private final SecurityService securityService;

    @PostMapping("/sync")
    public ResponseEntity<MobileOnboardingResponse> sync(@RequestBody MobileOnboardingSyncRequest request) {
        UserPrincipal principal = securityService.currentUser();
        MobileOnboardingResponse response = mobileOnboardingService.syncOnboarding(request, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<MobileOnboardingResponse> get(@RequestParam(required = false) String email) {
        UserPrincipal principal = securityService.currentUser();
        MobileOnboardingResponse response = mobileOnboardingService.getOnboarding(principal, email);
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.DeleteMapping
    public ResponseEntity<java.util.Map<String, Object>> reset(@RequestParam(required = false) String email) {
        UserPrincipal principal = securityService.currentUser();
        java.util.Map<String, Object> result = mobileOnboardingService.resetOnboarding(principal, email);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset")
    public ResponseEntity<java.util.Map<String, Object>> resetPost(@RequestParam(required = false) String email) {
        UserPrincipal principal = securityService.currentUser();
        java.util.Map<String, Object> result = mobileOnboardingService.resetOnboarding(principal, email);
        return ResponseEntity.ok(result);
    }
}
