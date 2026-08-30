package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.PaymentMethod;
import com.zeezaglobal.gymmanagement.entity.TransactionDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull TransactionDirection direction,
        @NotBlank String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull PaymentMethod paymentMethod,
        Long memberId,
        Long trainerId,
        Long managerId,
        @NotNull LocalDate occurredOn,
        String notes
) {
}
