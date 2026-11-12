package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.NutritionistInvite;
import com.zeezaglobal.gymmanagement.entity.TrainerInviteStatus;

import java.time.Instant;

public record NutritionistInviteResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        TrainerInviteStatus status,
        Instant createdAt,
        Instant expiresAt
) {
    public static NutritionistInviteResponse fromEntity(NutritionistInvite invite) {
        return new NutritionistInviteResponse(
                invite.getId(),
                invite.getEmail(),
                invite.getFirstName(),
                invite.getLastName(),
                invite.getStatus(),
                invite.getCreatedAt(),
                invite.getExpiresAt());
    }
}
