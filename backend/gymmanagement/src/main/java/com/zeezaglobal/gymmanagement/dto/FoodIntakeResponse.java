package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.FoodIntakeRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One food-log entry served back to the mobile app so it can show meals that
 * were logged elsewhere (another device, the diet website, or an MCP client)
 * alongside the ones logged on-device. {@code externalId} is the app's own
 * entry id, so the app merges by it and never duplicates a local entry. The
 * photo blob is not sent in the list for performance; {@code hasPhoto} tells
 * the app whether one exists on the server.
 */
public record FoodIntakeResponse(
        String externalId,
        LocalDate recordDate,
        String meal,
        String dishName,
        Integer calories,
        Double proteinG,
        Double carbsG,
        Double fatG,
        boolean hasPhoto,
        LocalDateTime updatedAt
) {
    public static FoodIntakeResponse fromEntity(FoodIntakeRecord r) {
        return new FoodIntakeResponse(
                r.getExternalId(),
                r.getRecordDate(),
                r.getMeal(),
                r.getDishName(),
                r.getCalories(),
                r.getProteinG(),
                r.getCarbsG(),
                r.getFatG(),
                r.getPhotoData() != null && r.getPhotoData().length > 0,
                r.getUpdatedAt()
        );
    }
}
