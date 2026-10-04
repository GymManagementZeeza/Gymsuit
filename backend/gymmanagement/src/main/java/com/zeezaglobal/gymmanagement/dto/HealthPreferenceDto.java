package com.zeezaglobal.gymmanagement.dto;

import java.util.List;

public record HealthPreferenceDto(
        String metricType,
        List<String> preferredDeviceTypes,
        List<String> preferredManufacturers
) {}
