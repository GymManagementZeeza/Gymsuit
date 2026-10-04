package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDateTime;

public record HealthRawRecordDto(
        String clientRecordId,
        String metricType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Double value,
        String unit,
        String payloadJson,
        String sourcePlatform,
        String sourceDeviceType,
        String sourceManufacturer,
        String sourceModel,
        String sourceSystem,
        String sourceAppId,
        String sourceRecordId,
        Double confidence
) {}
