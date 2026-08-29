package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.CheckIn;

import java.time.LocalDateTime;

public record CheckInResponse(
        Long id,
        Long gymId,
        Long memberId,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime
) {
    public static CheckInResponse fromEntity(CheckIn checkIn) {
        return new CheckInResponse(
                checkIn.getId(),
                checkIn.getGym().getId(),
                checkIn.getMember().getId(),
                checkIn.getCheckInTime(),
                checkIn.getCheckOutTime()
        );
    }
}
