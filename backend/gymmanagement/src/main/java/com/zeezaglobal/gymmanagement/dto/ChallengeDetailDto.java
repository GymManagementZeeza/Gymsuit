package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.ChallengeMetric;
import com.zeezaglobal.gymmanagement.entity.ChallengeStatus;

import java.time.LocalDate;
import java.util.List;

public record ChallengeDetailDto(
        Long id,
        String name,
        String description,
        ChallengeMetric metricType,
        LocalDate startDate,
        LocalDate endDate,
        ChallengeStatus status,
        long participantCount,
        Integer myRank,
        String inviteCode,
        boolean createdByMe,
        List<LeaderboardEntryDto> leaderboard
) {
}
