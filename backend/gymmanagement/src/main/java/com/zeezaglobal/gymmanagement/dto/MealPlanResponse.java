package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.MealPlan;

import java.time.Instant;
import java.time.LocalDate;

public record MealPlanResponse(
        Long id,
        LocalDate date,
        String nutritionistName,
        String breakfast,
        String lunch,
        String eveningSnack,
        String dinner,
        String note,
        Instant updatedAt
) {
    public static MealPlanResponse fromEntity(MealPlan plan) {
        String name = plan.getNutritionist().getFullName();
        return new MealPlanResponse(
                plan.getId(),
                plan.getPlanDate(),
                name != null && !name.isBlank() ? name : "Your nutritionist",
                plan.getBreakfast(),
                plan.getLunch(),
                plan.getEveningSnack(),
                plan.getDinner(),
                plan.getNote(),
                plan.getUpdatedAt());
    }
}
