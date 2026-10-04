package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HealthSyncResponse(
        LocalDateTime serverSyncTime,
        int uploadedDailyCount,
        int uploadedWorkoutCount,
        List<DailyHealthSyncDto> remoteDailyRecords,
        List<WorkoutSyncDto> remoteWorkouts,
        Integer pointsBalance,
        List<PointsTransactionDto> pointsTransactions
) {
    public HealthSyncResponse(LocalDateTime serverSyncTime, int uploadedDailyCount, int uploadedWorkoutCount,
                              List<DailyHealthSyncDto> remoteDailyRecords, List<WorkoutSyncDto> remoteWorkouts) {
        this(serverSyncTime, uploadedDailyCount, uploadedWorkoutCount, remoteDailyRecords, remoteWorkouts, null, null);
    }
}
