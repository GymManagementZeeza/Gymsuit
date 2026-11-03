package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ClientProgressResponse(
        Long memberId,
        String firstName,
        String lastName,
        String email,
        /** False when the member has no app login at all — nothing can be synced. */
        boolean hasAppAccount,
        /** False when no health, workout or intake record exists for them ever. */
        boolean hasAnyRecords,
        LocalDate from,
        LocalDate to,
        List<Day> days,
        List<Workout> workouts
) {
    public record Day(
            LocalDate date,
            Long steps,
            Double activeCalories,
            Double totalCalories,
            Double distanceMeters,
            Integer latestHeartRateBpm,
            Integer restingHeartRateBpm,
            Integer minHeartRateBpm,
            Integer maxHeartRateBpm,
            Long sleepDurationMinutes,
            LocalDateTime sleepStartTime,
            LocalDateTime sleepEndTime,
            Double weightKg,
            String sourceDevice,
            Integer intakeCalories,
            Double intakeProteinG,
            Double intakeCarbsG,
            Double intakeFatG,
            Integer workoutCount,
            Double workoutCalories
    ) {
    }

    public record Workout(
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer durationMinutes,
            Double caloriesBurned,
            Integer setCount,
            Integer totalReps,
            Double totalVolumeKg
    ) {
    }
}
