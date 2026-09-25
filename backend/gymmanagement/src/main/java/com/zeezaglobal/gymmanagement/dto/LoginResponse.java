package com.zeezaglobal.gymmanagement.dto;

public record LoginResponse(
        String token,
        String role,
        Long gymId,
        Long trainerId,
        Long memberId,
        Long managerId,
        /**
         * Raw refresh token for native clients. Web clients receive the same
         * value as an httpOnly cookie and can ignore this field.
         */
        String refreshToken
) {
}
