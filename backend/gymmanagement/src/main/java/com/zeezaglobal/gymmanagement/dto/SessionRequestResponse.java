package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.SessionRequest;

import java.time.LocalDateTime;

public record SessionRequestResponse(
        Long id,
        Long gymId,
        Long memberId,
        Long trainerId,
        LocalDateTime requestedStartTime,
        LocalDateTime requestedEndTime,
        String notes,
        String status,
        LocalDateTime createdAt,
        LocalDateTime respondedAt,
        String responseNote,
        Long sessionId
) {
    public static SessionRequestResponse fromEntity(SessionRequest request) {
        return new SessionRequestResponse(
                request.getId(),
                request.getGym().getId(),
                request.getMember().getId(),
                request.getTrainer().getId(),
                request.getRequestedStartTime(),
                request.getRequestedEndTime(),
                request.getNotes(),
                request.getStatus().name(),
                request.getCreatedAt(),
                request.getRespondedAt(),
                request.getResponseNote(),
                request.getSession() != null ? request.getSession().getId() : null
        );
    }
}
