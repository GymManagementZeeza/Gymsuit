package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record SessionRequest(
        @NotNull Long trainerId,
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime startTime,
        LocalDateTime endTime,
        @Positive Integer capacity
) {
}
