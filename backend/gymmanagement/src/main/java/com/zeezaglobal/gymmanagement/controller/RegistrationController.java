package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.LoginResponse;
import com.zeezaglobal.gymmanagement.dto.MemberRegisterRequest;
import com.zeezaglobal.gymmanagement.dto.MembershipPlanResponse;
import com.zeezaglobal.gymmanagement.dto.OwnerRegisterRequest;
import com.zeezaglobal.gymmanagement.security.RefreshCookieFactory;
import com.zeezaglobal.gymmanagement.service.MembershipPlanService;
import com.zeezaglobal.gymmanagement.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth/register")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;
    private final MembershipPlanService membershipPlanService;
    private final RefreshCookieFactory refreshCookieFactory;

    @PostMapping("/owner")
    public ResponseEntity<LoginResponse> registerOwner(@Valid @RequestBody OwnerRegisterRequest request) {
        return withRefreshCookie(registrationService.registerOwner(request));
    }

    @PostMapping("/member")
    public ResponseEntity<LoginResponse> registerMember(@Valid @RequestBody MemberRegisterRequest request) {
        return withRefreshCookie(registrationService.registerMember(request));
    }

    /** Public plan list a prospective member can pick from before they have an account — no auth yet. */
    @GetMapping("/member/plans/{gymId}")
    public List<MembershipPlanResponse> plansForGym(@PathVariable Long gymId) {
        return membershipPlanService.findActiveByGym(gymId);
    }

    private ResponseEntity<LoginResponse> withRefreshCookie(LoginResponse response) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.issue(response.refreshToken()).toString())
                .body(response);
    }
}
