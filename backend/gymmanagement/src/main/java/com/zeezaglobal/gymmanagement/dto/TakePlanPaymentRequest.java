package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record TakePlanPaymentRequest(
        @NotNull PaymentMethod paymentMethod
) {
}
