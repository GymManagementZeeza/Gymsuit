package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.LoginResponse;
import com.zeezaglobal.gymmanagement.dto.MemberRegisterRequest;
import com.zeezaglobal.gymmanagement.dto.MembershipPlanResponse;
import com.zeezaglobal.gymmanagement.dto.EmailRequest;
import com.zeezaglobal.gymmanagement.dto.NutritionistRegisterRequest;
import com.zeezaglobal.gymmanagement.security.AuthRateLimiter;
import com.zeezaglobal.gymmanagement.service.NutritionistService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final NutritionistService nutritionistService;
    private final AuthRateLimiter rateLimiter;

    @PostMapping("/owner")
    public ResponseEntity<LoginResponse> registerOwner(@Valid @RequestBody OwnerRegisterRequest request) {
        return withRefreshCookie(registrationService.registerOwner(request));
    }

    @PostMapping("/member")
    public ResponseEntity<LoginResponse> registerMember(@Valid @RequestBody MemberRegisterRequest request) {
        return withRefreshCookie(registrationService.registerMember(request));
    }

    /** Standalone nutrition expert sign-up, step 1: email a verification code to a new address. */
    @PostMapping("/nutritionist/code")
    public ResponseEntity<Void> sendNutritionistCode(@Valid @RequestBody EmailRequest request, HttpServletRequest httpRequest) {
        rateLimiter.check(AuthRateLimiter.Action.OTP_SEND, request.email(), httpRequest);
        nutritionistService.sendSignupCode(request.email());
        return ResponseEntity.accepted().build();
    }

    /** Step 2: verify the code and create the account (no gym). Signs the new nutritionist in. */
    @PostMapping("/nutritionist")
    public ResponseEntity<LoginResponse> registerNutritionist(
            @Valid @RequestBody NutritionistRegisterRequest request, HttpServletRequest httpRequest) {
        rateLimiter.check(AuthRateLimiter.Action.OTP_VERIFY, request.email(), httpRequest);
        return withRefreshCookie(nutritionistService.register(request));
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
