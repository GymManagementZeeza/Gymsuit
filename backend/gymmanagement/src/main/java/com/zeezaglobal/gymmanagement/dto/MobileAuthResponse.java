package com.zeezaglobal.gymmanagement.dto;

public record MobileAuthResponse(
        String token,
        String refreshToken,
        String role,
        String email,
        String firstName,
        String lastName,
        Long memberId,
        Long gymId,
        String gymName
) {
}
