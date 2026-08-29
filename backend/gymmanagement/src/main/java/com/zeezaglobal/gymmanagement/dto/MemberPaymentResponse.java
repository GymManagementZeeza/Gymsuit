package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.MemberPayment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MemberPaymentResponse(
        Long id,
        Long gymId,
        Long memberId,
        Long subscriptionId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String status,
        LocalDate periodStart,
        LocalDate periodEnd,
        String gatewayReference,
        LocalDateTime paidAt,
        String notes,
        LocalDateTime createdAt
) {
    public static MemberPaymentResponse fromEntity(MemberPayment payment) {
        return new MemberPaymentResponse(
                payment.getId(),
                payment.getGym().getId(),
                payment.getMember().getId(),
                payment.getSubscription() != null ? payment.getSubscription().getId() : null,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod().name(),
                payment.getStatus().name(),
                payment.getPeriodStart(),
                payment.getPeriodEnd(),
                payment.getGatewayReference(),
                payment.getPaidAt(),
                payment.getNotes(),
                payment.getCreatedAt()
        );
    }
}
