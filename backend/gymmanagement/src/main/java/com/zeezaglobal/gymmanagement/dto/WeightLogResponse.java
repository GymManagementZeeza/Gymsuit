package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.WeightLog;

import java.time.LocalDateTime;

public record WeightLogResponse(
        Long id,
        Double weightKg,
        LocalDateTime recordedAt
) {
    public static WeightLogResponse fromEntity(WeightLog log) {
        return new WeightLogResponse(log.getId(), log.getWeightKg(), log.getRecordedAt());
    }
}
