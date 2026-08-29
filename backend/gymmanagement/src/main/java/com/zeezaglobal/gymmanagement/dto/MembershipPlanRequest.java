package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.BillingCycle;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MembershipPlanRequest(
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull BillingCycle billingCycle,
        Boolean active
) {
}
