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
        List<WorkoutSyncDto> workouts
) {}
