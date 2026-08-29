package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record MemberGoalRequest(
        @NotNull @Positive Double goalWeightKg,
        @NotNull @FutureOrPresent LocalDate goalTargetDate
) {
}
