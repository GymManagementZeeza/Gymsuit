package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDateTime;

public record HealthCanonicalRecordDto(
        String id,
        String metricType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Double value,
        String unit,
        String payloadJson,
        String selectedSourceSystem,
        String selectedDeviceId,
        Double confidence,
        Long serverVersion,
        boolean isDeleted
) {}
