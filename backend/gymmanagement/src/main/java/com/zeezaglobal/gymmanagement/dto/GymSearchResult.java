package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.Gym;

public record GymSearchResult(
        Long id,
        String name,
        String city,
        String state
) {
    public static GymSearchResult fromEntity(Gym gym) {
        return new GymSearchResult(gym.getId(), gym.getName(), gym.getCity(), gym.getState());
    }
}
