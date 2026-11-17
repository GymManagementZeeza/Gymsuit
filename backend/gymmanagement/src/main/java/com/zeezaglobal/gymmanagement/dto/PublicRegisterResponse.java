package com.zeezaglobal.gymmanagement.dto;

public record PublicRegisterResponse(
        Long memberId,
        Long subscriptionId,
        String status,
        String planName
) {
}
