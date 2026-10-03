package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.ChallengeMetric;

import java.time.LocalDate;

public record ChallengeCreateRequest(
        String name,
        String description,
        ChallengeMetric metricType,
        LocalDate startDate,
        LocalDate endDate
) {
}
