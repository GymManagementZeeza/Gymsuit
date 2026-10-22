package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDate;

public record MobileOnboardingSyncRequest(
        String workoutGoal,
        Double heightCm,
        Double weightKg,
        Double targetWeightKg,
        Integer birthYear,
        LocalDate dateOfBirth,
        Integer workoutDaysPerWeek,
        LocalDate targetDate,
        String email
) {
}
