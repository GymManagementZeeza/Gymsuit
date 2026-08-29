package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MemberWeightRequest(
        @NotNull @Positive Double weightKg
) {
}
