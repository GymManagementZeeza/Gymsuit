package com.zeezaglobal.gymmanagement.dto;

import java.util.List;

public record HealthV1SyncRequest(
        String deviceId,
        Long lastSyncVersion,
        List<HealthRawRecordDto> rawRecords,
        List<String> deletedRecordIds,
        List<DailyHealthSyncDto> dailyRecords,
        List<WorkoutSyncDto> workouts
) {}
