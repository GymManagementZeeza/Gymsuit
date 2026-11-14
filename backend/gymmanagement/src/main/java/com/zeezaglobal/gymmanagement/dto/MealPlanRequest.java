package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Meal recommendations for one day. At least one meal must be filled in (checked in the service). */
public record MealPlanRequest(
        @NotNull LocalDate date,
        @Size(max = 2000) String breakfast,
        @Size(max = 2000) String lunch,
        @Size(max = 2000) String eveningSnack,
        @Size(max = 2000) String dinner,
        @Size(max = 2000) String note
) {
}
