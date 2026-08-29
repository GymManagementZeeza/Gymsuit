package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.SessionEnrollment;

import java.time.LocalDateTime;

public record SessionEnrollmentResponse(
        Long id,
        Long sessionId,
        Long memberId,
        LocalDateTime assignedAt
) {
    public static SessionEnrollmentResponse fromEntity(SessionEnrollment enrollment) {
        return new SessionEnrollmentResponse(
                enrollment.getId(),
                enrollment.getSession().getId(),
                enrollment.getMember().getId(),
                enrollment.getAssignedAt()
        );
    }
}
