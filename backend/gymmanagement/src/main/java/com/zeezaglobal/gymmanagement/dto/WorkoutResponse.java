package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.WorkoutRecord;

import java.time.LocalDateTime;

/**
 * One workout session served back to the apps so they can show workouts
 * logged elsewhere (another device, an MCP client) alongside on-device ones.
 * {@code externalId} is the caller's entry id, so the app merges by it.
 */
public record WorkoutResponse(
        String externalId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer durationMinutes,
        Double caloriesBurned,
        Integer setCount,
        Integer totalReps,
        Double totalVolumeKg,
        String sourceDevice,
        LocalDateTime updatedAt
) {
    public static WorkoutResponse fromEntity(WorkoutRecord w) {
        return new WorkoutResponse(
                w.getExternalId(), w.getTitle(), w.getStartTime(), w.getEndTime(),
                w.getDurationMinutes(), w.getCaloriesBurned(), w.getSetCount(),
                w.getTotalReps(), w.getTotalVolumeKg(), w.getSourceDevice(), w.getUpdatedAt());
    }
}
