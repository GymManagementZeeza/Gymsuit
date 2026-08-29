package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record PaymentStatusUpdateRequest(
        @NotNull PaymentStatus status
) {
}
