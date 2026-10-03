package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.ChallengeMetric;
import com.zeezaglobal.gymmanagement.entity.ChallengeStatus;

import java.time.LocalDate;

public record ChallengeSummaryDto(
        Long id,
        String name,
        ChallengeMetric metricType,
        LocalDate startDate,
        LocalDate endDate,
        ChallengeStatus status,
        long participantCount,
        Integer myRank
) {
}
