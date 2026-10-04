package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DailyHealthSyncDto(
        LocalDate recordDate,
        Long steps,
        Double activeCalories,
        Double totalCalories,
        Double distanceMeters,
        Integer latestHeartRateBpm,
        Integer restingHeartRateBpm,
        Integer minHeartRateBpm,
        Integer maxHeartRateBpm,
        String heartRateSamplesJson,
        Long sleepDurationMinutes,
        LocalDateTime sleepStartTime,
        LocalDateTime sleepEndTime,
        String sleepStagesJson,
        Double weightKg,
        String sourceDevice,
        LocalDateTime updatedAt
) {}
