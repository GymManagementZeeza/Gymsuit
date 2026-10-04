package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HealthSyncResponse(
        LocalDateTime serverSyncTime,
        int uploadedDailyCount,
        int uploadedWorkoutCount,
        List<DailyHealthSyncDto> remoteDailyRecords,
        List<WorkoutSyncDto> remoteWorkouts
) {}
