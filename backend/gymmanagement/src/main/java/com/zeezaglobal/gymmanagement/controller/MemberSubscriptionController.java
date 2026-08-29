package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.MemberSubscriptionResponse;
import com.zeezaglobal.gymmanagement.dto.SubscribeRequest;
import com.zeezaglobal.gymmanagement.service.MemberSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gyms/{gymId}/members/{memberId}/subscriptions")
@RequiredArgsConstructor
public class MemberSubscriptionController {

    private final MemberSubscriptionService subscriptionService;

    @GetMapping
    @PreAuthorize("@security.canManageMemberBilling(#gymId, #memberId)")
    public List<MemberSubscriptionResponse> history(@PathVariable Long gymId, @PathVariable Long memberId) {
        return subscriptionService.history(gymId, memberId);
    }

    @GetMapping("/current")
    @PreAuthorize("@security.canManageMemberBilling(#gymId, #memberId)")
    public MemberSubscriptionResponse current(@PathVariable Long gymId, @PathVariable Long memberId) {
        return subscriptionService.current(gymId, memberId);
    }

    @PostMapping
    @PreAuthorize("@security.canManageMemberBilling(#gymId, #memberId)")
    public ResponseEntity<MemberSubscriptionResponse> subscribe(
            @PathVariable Long gymId, @PathVariable Long memberId, @Valid @RequestBody SubscribeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.subscribe(gymId, memberId, request));
    }

    @PutMapping("/current/cancel")
    @PreAuthorize("@security.canManageMemberBilling(#gymId, #memberId)")
    public MemberSubscriptionResponse cancel(@PathVariable Long gymId, @PathVariable Long memberId) {
        return subscriptionService.cancel(gymId, memberId);
    }

    @PutMapping("/current/change-plan")
    @PreAuthorize("@security.canManageMemberBilling(#gymId, #memberId)")
    public MemberSubscriptionResponse changePlan(
            @PathVariable Long gymId, @PathVariable Long memberId, @Valid @RequestBody SubscribeRequest request) {
        return subscriptionService.changePlan(gymId, memberId, request);
    }
}
