package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.GymTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long gymId,
        Long memberId,
        Long trainerId,
        String direction,
        String description,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        LocalDate occurredOn,
        String notes,
        LocalDateTime createdAt
) {
    public static TransactionResponse fromEntity(GymTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getGym().getId(),
                transaction.getMember() != null ? transaction.getMember().getId() : null,
                transaction.getTrainer() != null ? transaction.getTrainer().getId() : null,
                transaction.getDirection().name(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getPaymentMethod().name(),
                transaction.getOccurredOn(),
                transaction.getNotes(),
                transaction.getCreatedAt()
        );
    }
}
