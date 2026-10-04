package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HealthSyncRequest(
        LocalDateTime lastSyncTime,
        String clientDevice, // "IOS_APPLE_HEALTH" or "ANDROID_HEALTH_CONNECT"
        List<DailyHealthSyncDto> dailyRecords,
        List<WorkoutSyncDto> workouts,
        List<PointsTransactionDto> pointsTransactions
) {
    public HealthSyncRequest(LocalDateTime lastSyncTime, String clientDevice,
                             List<DailyHealthSyncDto> dailyRecords, List<WorkoutSyncDto> workouts) {
        this(lastSyncTime, clientDevice, dailyRecords, workouts, null);
    }
}
