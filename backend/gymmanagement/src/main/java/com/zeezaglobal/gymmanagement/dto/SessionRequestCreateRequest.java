package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SessionRequestCreateRequest(
        @NotNull Long trainerId,
        @NotNull LocalDateTime requestedStartTime,
        LocalDateTime requestedEndTime,
        String notes
) {
}
