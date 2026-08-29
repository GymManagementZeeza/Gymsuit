package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.MemberSubscription;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MemberSubscriptionResponse(
        Long id,
        Long gymId,
        Long memberId,
        Long planId,
        String planName,
        BigDecimal planPrice,
        String planCurrency,
        String planBillingCycle,
        String status,
        LocalDate startDate,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        boolean autoRenew
) {
    public static MemberSubscriptionResponse fromEntity(MemberSubscription subscription) {
        return new MemberSubscriptionResponse(
                subscription.getId(),
                subscription.getGym().getId(),
                subscription.getMember().getId(),
                subscription.getPlan().getId(),
                subscription.getPlan().getName(),
                subscription.getPlan().getPrice(),
                subscription.getPlan().getCurrency(),
                subscription.getPlan().getBillingCycle().name(),
                subscription.getStatus().name(),
                subscription.getStartDate(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.isAutoRenew()
        );
    }
}
