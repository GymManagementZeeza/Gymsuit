package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record JoiningFeePaymentRequest(
        @NotNull PaymentMethod paymentMethod,
        String notes
) {
}
