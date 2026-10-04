package com.zeezaglobal.gymmanagement.dto;

public record DeviceRegistrationRequest(
        String deviceId,
        String platform,
        String deviceType,
        String manufacturer,
        String model,
        String osVersion,
        String appVersion,
        String healthSource
) {}
