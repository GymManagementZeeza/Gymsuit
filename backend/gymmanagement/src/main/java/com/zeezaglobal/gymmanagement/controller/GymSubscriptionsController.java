package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.MemberSubscriptionResponse;
import com.zeezaglobal.gymmanagement.service.MemberSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gyms/{gymId}/subscriptions")
@RequiredArgsConstructor
public class GymSubscriptionsController {

    private final MemberSubscriptionService subscriptionService;

    @GetMapping("/current")
    @PreAuthorize("@security.canViewGymActivity(#gymId)")
    public List<MemberSubscriptionResponse> currentForGym(@PathVariable Long gymId) {
        return subscriptionService.currentForGym(gymId);
    }

    @GetMapping("/pending")
    @PreAuthorize("@security.canViewGymActivity(#gymId)")
    public List<MemberSubscriptionResponse> pendingForGym(@PathVariable Long gymId) {
        return subscriptionService.pendingForGym(gymId);
    }
}
