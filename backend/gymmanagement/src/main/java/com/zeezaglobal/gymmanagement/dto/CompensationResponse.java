package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.ManagerCompensation;
import com.zeezaglobal.gymmanagement.entity.TrainerCompensation;

import java.math.BigDecimal;

public record CompensationResponse(
        String payType,
        BigDecimal hourlyRate,
        BigDecimal monthlySalary,
        String currency
) {
    public static CompensationResponse fromEntity(TrainerCompensation compensation) {
        return new CompensationResponse(
                compensation.getPayType().name(),
                compensation.getHourlyRate(),
                compensation.getMonthlySalary(),
                compensation.getCurrency()
        );
    }

    public static CompensationResponse fromEntity(ManagerCompensation compensation) {
        return new CompensationResponse(
                compensation.getPayType().name(),
                compensation.getHourlyRate(),
                compensation.getMonthlySalary(),
                compensation.getCurrency()
        );
    }
}
