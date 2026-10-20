package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.StepBadgeDto;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.service.StepBadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Serves the signed-in user's step milestone badges, newest first.
 * Same authentication pattern as the other /api/mobile endpoints.
 */
@RestController
@RequestMapping("/api/mobile/badges")
@RequiredArgsConstructor
public class StepBadgeController {

    private final StepBadgeService stepBadgeService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<StepBadgeDto>> getBadges(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(stepBadgeService.getBadges(user));
    }
}
