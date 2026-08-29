package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.GymActivityResponse;
import com.zeezaglobal.gymmanagement.entity.ActivityType;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.GymActivity;
import com.zeezaglobal.gymmanagement.repository.GymActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GymActivityService {

    private final GymActivityRepository activityRepository;
    private final GymService gymService;

    public void record(Gym gym, ActivityType type, String message) {
        GymActivity activity = new GymActivity();
        activity.setGym(gym);
        activity.setType(type);
        activity.setMessage(message);
        activity.setCreatedAt(LocalDateTime.now());
        activityRepository.save(activity);
    }

    public List<GymActivityResponse> recentForGym(Long gymId) {
        gymService.getGymOrThrow(gymId);
        return activityRepository.findTop30ByGymIdOrderByCreatedAtDesc(gymId).stream()
                .map(GymActivityResponse::fromEntity)
                .toList();
    }
}
