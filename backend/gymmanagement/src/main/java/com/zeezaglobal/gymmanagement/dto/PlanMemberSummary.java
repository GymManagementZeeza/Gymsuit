package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.MemberSubscription;

public record PlanMemberSummary(
        Long memberId,
        String firstName,
        String lastName,
        String status
) {
    public static PlanMemberSummary fromEntity(MemberSubscription subscription) {
        return new PlanMemberSummary(
                subscription.getMember().getId(),
                subscription.getMember().getFirstName(),
                subscription.getMember().getLastName(),
                subscription.getStatus().name()
        );
    }
}
