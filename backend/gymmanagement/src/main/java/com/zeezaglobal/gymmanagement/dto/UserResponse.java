package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.User;

public record UserResponse(
        Long id,
        String email,
        String role,
        Long gymId,
        Long trainerId,
        Long memberId,
        Long managerId,
        boolean enabled
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getGym() != null ? user.getGym().getId() : null,
                user.getTrainer() != null ? user.getTrainer().getId() : null,
                user.getMember() != null ? user.getMember().getId() : null,
                user.getManager() != null ? user.getManager().getId() : null,
                user.isEnabled()
        );
    }
}
