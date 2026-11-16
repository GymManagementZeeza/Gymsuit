package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mobile / MCP workout-log upload. Mirrors {@link FoodIntakeSyncRequest}: the
 * backend upserts a {@code WorkoutRecord} by the caller's entry id, so
 * re-sending never double-counts. Strength entries carry setCount / totalReps /
 * totalVolumeKg; cardio entries carry duration (and leave those null).
 * {@code caloriesBurned} is optional — when absent the backend estimates it
 * from the activity and the member's latest known weight.
 */
public record WorkoutSyncRequest(
        @Valid List<Entry> entries
) {
    public record Entry(
            @NotBlank String externalId,
            @NotBlank String title,
            @NotNull LocalDateTime startTime,
            LocalDateTime endTime,
            Integer durationMinutes,
            Double caloriesBurned,
            Integer setCount,
            Integer totalReps,
            Double totalVolumeKg,
            /** Optional hint for the calorie estimate, e.g. RUNNING / CYCLING / STRENGTH. */
            String activityType,
            /** Optional source label, e.g. "MCP"; defaults to "MCP" for MCP uploads. */
            String sourceDevice
    ) {
    }
}
