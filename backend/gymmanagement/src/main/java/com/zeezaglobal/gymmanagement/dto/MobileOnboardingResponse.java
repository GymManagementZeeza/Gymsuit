package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDate;

public record MobileOnboardingResponse(
        Long memberId,
        String workoutGoal,
        Double heightCm,
        Double weightKg,
        Double targetWeightKg,
        Integer birthYear,
        LocalDate dateOfBirth,
        Integer workoutDaysPerWeek,
        LocalDate targetDate,
        String message
) {
}
