package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDateTime;

public record HealthTombstoneDto(
        String canonicalRecordId,
        String metricType,
        LocalDateTime deletedAt,
        Long serverVersion
) {}
