package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.TrainingSession;

import java.time.LocalDateTime;

public record SessionResponse(
        Long id,
        Long gymId,
        Long trainerId,
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer capacity,
        long enrolledCount
) {
    public static SessionResponse fromEntity(TrainingSession session, long enrolledCount) {
        return new SessionResponse(
                session.getId(),
                session.getGym().getId(),
                session.getTrainer().getId(),
                session.getTitle(),
                session.getDescription(),
                session.getStartTime(),
                session.getEndTime(),
                session.getCapacity(),
                enrolledCount
        );
    }
}
