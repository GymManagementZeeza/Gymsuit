package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.TrainerInvite;
import com.zeezaglobal.gymmanagement.entity.TrainerInviteStatus;

import java.time.Instant;

/**
 * What the public accept page may show about an invite: gym + trainer names,
 * the invited email, and whether it is still usable. No member data.
 */
public record TrainerInvitePreviewResponse(
        String gymName,
        String trainerName,
        String email,
        String firstName,
        String lastName,
        TrainerInviteStatus status,
        Instant expiresAt,
        /** True when the invitee already has a member record at this gym. */
        boolean existingMember
) {
    public static TrainerInvitePreviewResponse fromEntity(
            TrainerInvite invite, String gymName, String trainerName, boolean existingMember) {
        return new TrainerInvitePreviewResponse(
                gymName,
                trainerName,
                invite.getEmail(),
                invite.getFirstName(),
                invite.getLastName(),
                invite.getStatus(),
                invite.getExpiresAt(),
                existingMember);
    }
}
