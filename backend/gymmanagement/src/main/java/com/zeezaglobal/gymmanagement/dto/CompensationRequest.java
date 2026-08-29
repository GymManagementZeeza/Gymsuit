package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.PayType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CompensationRequest(
        @NotNull PayType payType,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal hourlyRate,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal monthlySalary,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}
