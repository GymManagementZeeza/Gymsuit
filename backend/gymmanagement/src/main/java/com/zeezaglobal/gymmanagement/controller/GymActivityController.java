package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.GymActivityResponse;
import com.zeezaglobal.gymmanagement.service.GymActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gyms/{gymId}/activity")
@RequiredArgsConstructor
public class GymActivityController {

    private final GymActivityService activityService;

    @GetMapping
    @PreAuthorize("@security.canViewGymActivity(#gymId)")
    public List<GymActivityResponse> recent(@PathVariable Long gymId) {
        return activityService.recentForGym(gymId);
    }
}
