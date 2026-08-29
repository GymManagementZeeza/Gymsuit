package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long gymId,
        String payeeType,
        Long trainerId,
        Long managerId,
        BigDecimal amount,
        String currency,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal hoursWorked,
        String status,
        LocalDateTime paidAt,
        String notes,
        LocalDateTime createdAt
) {
    public static PaymentResponse fromEntity(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getGym().getId(),
                payment.getPayeeType().name(),
                payment.getTrainer() != null ? payment.getTrainer().getId() : null,
                payment.getManager() != null ? payment.getManager().getId() : null,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPeriodStart(),
                payment.getPeriodEnd(),
                payment.getHoursWorked(),
                payment.getStatus().name(),
                payment.getPaidAt(),
                payment.getNotes(),
                payment.getCreatedAt()
        );
    }
}
