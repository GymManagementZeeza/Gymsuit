package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDateTime;

public record PointsTransactionDto(
        String id,
        int points,
        String reason,
        Double rupeeValue,
        LocalDateTime createdAt,
        String deviceId
) {}
