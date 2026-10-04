package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDateTime;

public record WorkoutSyncDto(
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
) {}
