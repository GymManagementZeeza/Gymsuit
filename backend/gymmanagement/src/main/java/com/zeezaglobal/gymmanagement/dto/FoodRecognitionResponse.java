package com.zeezaglobal.gymmanagement.dto;

import java.util.Map;

/** Dish prediction and nutrition returned by POST /api/mobile/food/recognize. */
public record FoodRecognitionResponse(
        String dish,
        double confidence,
        Map<String, Double> probabilities,
        NutritionInfo nutrition
) {
    public FoodRecognitionResponse(String dish, double confidence, Map<String, Double> probabilities) {
        this(dish, confidence, probabilities, null);
    }

    public record NutritionInfo(
            String name,
            String serving,
            int kcal,
            int proteinG,
            int carbsG,
            int fatG
    ) {}
}
