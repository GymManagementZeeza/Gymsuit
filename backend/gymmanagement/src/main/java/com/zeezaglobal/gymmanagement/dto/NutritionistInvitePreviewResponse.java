package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.TrainerInviteStatus;

import java.time.Instant;

/** What the public accept page may show: who invited, which email, and whether that email has an app account. */
public record NutritionistInvitePreviewResponse(
        String nutritionistName,
        String email,
        TrainerInviteStatus status,
        Instant expiresAt,
        /** True when a GymSuit member account with this email exists, so accepting can link it right away. */
        boolean hasAccount
) {
}
