package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HealthV1SyncResponse(
        Long serverVersion,
        LocalDateTime timestamp,
        int uploadedCount,
        int reconciledCount,
        List<HealthCanonicalRecordDto> canonicalRecords,
        List<HealthTombstoneDto> tombstones,
        List<DailyHealthSyncDto> dailyRecords,
        List<WorkoutSyncDto> workouts,
        Integer pointsBalance,
        List<PointsTransactionDto> pointsTransactions
) {
    public HealthV1SyncResponse(Long serverVersion, LocalDateTime timestamp, int uploadedCount, int reconciledCount,
                                List<HealthCanonicalRecordDto> canonicalRecords, List<HealthTombstoneDto> tombstones,
                                List<DailyHealthSyncDto> dailyRecords, List<WorkoutSyncDto> workouts) {
        this(serverVersion, timestamp, uploadedCount, reconciledCount, canonicalRecords, tombstones, dailyRecords, workouts, null, null);
    }
}
