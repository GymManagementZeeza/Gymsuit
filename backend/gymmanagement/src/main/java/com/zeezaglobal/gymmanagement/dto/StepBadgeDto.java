package com.zeezaglobal.gymmanagement.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A step milestone badge as served to the mobile apps. {@code date} is the
 * walking day the badge was earned for (yyyy-MM-dd); {@code awardedAt} is the
 * moment the backend granted it (ISO instant).
 */
public record StepBadgeDto(
        Long id,
        int threshold,
        String badgeName,
        long stepsAchieved,
        LocalDate date,
        Instant awardedAt
) {
}
