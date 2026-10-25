package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.TrainerWorkoutSuggestionRequest;
import com.zeezaglobal.gymmanagement.dto.TrainerWorkoutSuggestionResponse;
import com.zeezaglobal.gymmanagement.service.TrainerWorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Trainer dashboard (web): a trainer sends today's workout to one of their clients. */
@RestController
@RequestMapping("/api/gyms/{gymId}/trainers/{trainerId}/clients/{memberId}/workout-suggestions")
@RequiredArgsConstructor
public class TrainerWorkoutController {

    private final TrainerWorkoutService trainerWorkoutService;

    @PostMapping
    @PreAuthorize("@security.hasGymAccess(#gymId)")
    public ResponseEntity<TrainerWorkoutSuggestionResponse> sendTodayWorkout(
            @PathVariable Long gymId,
            @PathVariable Long trainerId,
            @PathVariable Long memberId,
            @Valid @RequestBody TrainerWorkoutSuggestionRequest request) {
        return ResponseEntity.ok(trainerWorkoutService.sendSuggestion(gymId, trainerId, memberId, request));
    }
}
