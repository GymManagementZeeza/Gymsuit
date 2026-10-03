package com.zeezaglobal.gymmanagement.dto;

public record LeaderboardEntryDto(
        Long userId,
        String displayName,
        double value,
        int rank
) {
}
