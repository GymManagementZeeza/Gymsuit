package com.zeezaglobal.gymmanagement.dto;

public record DeviceRegistrationResponse(
        String deviceId,
        boolean registered,
        String message
) {}
