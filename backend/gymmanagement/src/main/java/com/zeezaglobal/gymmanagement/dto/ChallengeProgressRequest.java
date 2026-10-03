package com.zeezaglobal.gymmanagement.dto;

public record ChallengeProgressRequest(
        Long steps,
        Integer workouts,
        Double calories,
        Double distanceKm
) {
}
