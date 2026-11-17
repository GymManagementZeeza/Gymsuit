package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Free-text description of what the member did; {@code date} defaults to today. */
public record WorkoutAiLogRequest(
        @NotBlank @Size(max = 2000) String text,
        LocalDate date
) {
}
