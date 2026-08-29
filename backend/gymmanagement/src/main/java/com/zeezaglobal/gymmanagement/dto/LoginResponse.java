package com.zeezaglobal.gymmanagement.dto;

public record LoginResponse(
        String token,
        String role,
        Long gymId,
        Long trainerId,
        Long memberId,
        Long managerId
) {
}
