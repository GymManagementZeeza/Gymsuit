package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.PayeeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
        @NotNull PayeeType payeeType,
        Long trainerId,
        Long managerId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal hoursWorked,
        String notes
) {
}
