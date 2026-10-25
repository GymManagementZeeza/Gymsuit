package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.TrainerWorkoutSuggestionResponse;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.service.TrainerWorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mobile apps: the signed-in member reads the workout their trainer sent for today. */
@RestController
@RequestMapping("/api/mobile/trainer-workout")
@RequiredArgsConstructor
public class MobileTrainerWorkoutController {

    private final TrainerWorkoutService trainerWorkoutService;
    private final SecurityService securityService;

    @GetMapping("/today")
    public ResponseEntity<TrainerWorkoutSuggestionResponse> today() {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (principal.getMemberId() == null) {
            return ResponseEntity.notFound().build();
        }
        return trainerWorkoutService.getTodaySuggestion(principal.getMemberId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
