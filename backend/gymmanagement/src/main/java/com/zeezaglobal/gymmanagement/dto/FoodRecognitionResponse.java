package com.zeezaglobal.gymmanagement.dto;

import java.util.Map;

/** Dish prediction returned by POST /api/mobile/food/recognize. */
public record FoodRecognitionResponse(
        String dish,
        double confidence,
        Map<String, Double> probabilities
) {
}
