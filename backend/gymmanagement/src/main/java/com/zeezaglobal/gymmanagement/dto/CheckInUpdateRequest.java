package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CheckInUpdateRequest(
        @NotNull LocalDateTime checkInTime,
        LocalDateTime checkOutTime
) {
}
