package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.NutritionistClient;

import java.time.Instant;

/** What a nutritionist sees about a linked client in their list — no gym, payment or medical data. */
public record NutritionistClientResponse(
        Long memberId,
        Long gymId,
        String firstName,
        String lastName,
        String email,
        Double weightKg,
        Double goalWeightKg,
        Instant linkedAt
) {
    public static NutritionistClientResponse fromEntity(NutritionistClient link) {
        return fromEntity(link, null);
    }

    /** latestSyncedWeightKg (from the app's health sync) takes priority over the profile weight; may be null. */
    public static NutritionistClientResponse fromEntity(NutritionistClient link, Double latestSyncedWeightKg) {
        Member m = link.getMember();
        return new NutritionistClientResponse(
                m.getId(),
                m.getGym() != null ? m.getGym().getId() : null,
                m.getFirstName(),
                m.getLastName(),
                m.getEmail(),
                latestSyncedWeightKg != null ? latestSyncedWeightKg : m.getWeightKg(),
                m.getGoalWeightKg(),
                link.getLinkedAt());
    }
}
