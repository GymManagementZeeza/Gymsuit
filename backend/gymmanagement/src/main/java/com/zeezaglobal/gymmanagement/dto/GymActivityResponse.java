package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.GymActivity;

import java.time.LocalDateTime;

public record GymActivityResponse(
        Long id,
        String type,
        String message,
        LocalDateTime createdAt
) {
    public static GymActivityResponse fromEntity(GymActivity activity) {
        return new GymActivityResponse(
                activity.getId(),
                activity.getType().name(),
                activity.getMessage(),
                activity.getCreatedAt()
        );
    }
}
