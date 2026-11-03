package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.TrainerInvite;
import com.zeezaglobal.gymmanagement.entity.TrainerInviteStatus;

import java.time.Instant;

public record TrainerInviteResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        TrainerInviteStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant acceptedAt
) {
    public static TrainerInviteResponse fromEntity(TrainerInvite invite) {
        return new TrainerInviteResponse(
                invite.getId(),
                invite.getEmail(),
                invite.getFirstName(),
                invite.getLastName(),
                invite.getStatus(),
                invite.getCreatedAt(),
                invite.getExpiresAt(),
                invite.getAcceptedAt());
    }
}
