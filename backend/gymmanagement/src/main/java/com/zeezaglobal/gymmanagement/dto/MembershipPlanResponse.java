package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.MembershipPlan;

import java.math.BigDecimal;

public record MembershipPlanResponse(
        Long id,
        Long gymId,
        String name,
        String description,
        BigDecimal price,
        String currency,
        String billingCycle,
        boolean active
) {
    public static MembershipPlanResponse fromEntity(MembershipPlan plan) {
        return new MembershipPlanResponse(
                plan.getId(),
                plan.getGym().getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getCurrency(),
                plan.getBillingCycle().name(),
                plan.isActive()
        );
    }
}
