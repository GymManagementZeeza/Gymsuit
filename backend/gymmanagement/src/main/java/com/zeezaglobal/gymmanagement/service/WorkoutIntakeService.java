package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.WorkoutResponse;
import com.zeezaglobal.gymmanagement.dto.WorkoutSyncRequest;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.entity.WorkoutRecord;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.DailyHealthRecordRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.repository.WorkoutRecordRepository;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mobile / MCP workout-log sync. Mirrors {@link FoodIntakeService}: upserts a
 * {@link WorkoutRecord} per entry (keyed by user + externalId), lists a recent
 * window for the apps to pull, and deletes by externalId. When an entry has no
 * caloriesBurned, it is estimated from the activity (MET) and the member's
 * latest known weight, so an MCP-logged exercise still carries a calorie burn.
 */
@Service
@RequiredArgsConstructor
public class WorkoutIntakeService {

    private final WorkoutRecordRepository workoutRecordRepository;
    private final UserRepository userRepository;
    private final DailyHealthRecordRepository dailyHealthRecordRepository;

    private static final double DEFAULT_WEIGHT_KG = 70.0;

    @Transactional
    public List<WorkoutResponse> sync(WorkoutSyncRequest request, UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<WorkoutResponse> saved = new ArrayList<>();
        if (request.entries() == null) {
            return saved;
        }
        double weight = latestWeightKg(user.getId());
        for (WorkoutSyncRequest.Entry entry : request.entries()) {
            WorkoutRecord record = workoutRecordRepository
                    .findByUserIdAndExternalId(user.getId(), entry.externalId())
                    .orElseGet(() -> {
                        WorkoutRecord created = new WorkoutRecord();
                        created.setUser(user);
                        created.setExternalId(entry.externalId());
                        return created;
                    });
            record.setTitle(entry.title());
            LocalDateTime start = entry.startTime();
            Integer duration = entry.durationMinutes();
            LocalDateTime end = entry.endTime() != null
                    ? entry.endTime()
                    : start.plusMinutes(Math.max(duration != null ? duration : 1, 1));
            if (duration == null && entry.endTime() != null) {
                duration = (int) java.time.Duration.between(start, end).toMinutes();
            }
            record.setStartTime(start);
            record.setEndTime(end);
            record.setDurationMinutes(duration);
            record.setSetCount(entry.setCount());
            record.setTotalReps(entry.totalReps());
            record.setTotalVolumeKg(entry.totalVolumeKg());
            double calories = entry.caloriesBurned() != null && entry.caloriesBurned() > 0
                    ? entry.caloriesBurned()
                    : estimateCalories(entry.activityType(), entry.title(), duration, weight);
            record.setCaloriesBurned(Math.round(calories * 10.0) / 10.0);
            record.setSourceDevice(entry.sourceDevice() != null && !entry.sourceDevice().isBlank()
                    ? entry.sourceDevice() : "MCP");
            record.setUpdatedAt(LocalDateTime.now());
            workoutRecordRepository.save(record);
            saved.add(WorkoutResponse.fromEntity(record));
        }
        return saved;
    }

    /**
     * The member's workouts from {@code from} (default 30 days ago) through
     * today, newest first, so the app can merge workouts logged elsewhere.
     */
    @Transactional(readOnly = true)
    public List<WorkoutResponse> listForUser(UserPrincipal principal, LocalDate from) {
        LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = LocalDate.now().plusDays(1).atStartOfDay();
        return workoutRecordRepository
                .findByUserIdAndStartTimeBetweenOrderByStartTimeDesc(principal.getUserId(), startDt, endDt)
                .stream()
                .map(WorkoutResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void deleteByExternalId(String externalId, UserPrincipal principal) {
        workoutRecordRepository.findByUserIdAndExternalId(principal.getUserId(), externalId)
                .ifPresent(workoutRecordRepository::delete);
    }

    private double latestWeightKg(Long userId) {
        return dailyHealthRecordRepository
                .findTopByUserIdAndWeightKgIsNotNullOrderByRecordDateDesc(userId)
                .map(r -> r.getWeightKg())
                .filter(w -> w != null && w > 0)
                .orElse(DEFAULT_WEIGHT_KG);
    }

    /** calories = MET x weightKg x hours. Duration defaults to 30 min when unknown. */
    private double estimateCalories(String activityType, String title, Integer durationMinutes, double weightKg) {
        int minutes = durationMinutes != null && durationMinutes > 0 ? durationMinutes : 30;
        double met = metFor(activityType != null && !activityType.isBlank() ? activityType : title);
        return met * weightKg * (minutes / 60.0);
    }

    /** Rough MET by activity keyword (ACSM compendium), defaulting to resistance training. */
    private double metFor(String raw) {
        String t = raw == null ? "" : raw.toLowerCase();
        if (containsAny(t, "run", "jog", "sprint")) return 9.8;
        if (containsAny(t, "cycl", "bike", "biking", "spin")) return 7.5;
        if (containsAny(t, "swim")) return 8.0;
        if (containsAny(t, "walk", "hike", "hiking")) return 3.8;
        if (containsAny(t, "row")) return 7.0;
        if (containsAny(t, "ellipt")) return 5.0;
        if (containsAny(t, "hiit", "interval", "burpee", "jump")) return 8.0;
        if (containsAny(t, "yoga", "stretch", "pilates", "mobility")) return 3.0;
        if (containsAny(t, "dance", "zumba", "aerobic")) return 6.5;
        // Strength / resistance training (bench press, squat, deadlift, curls, ...).
        return 5.0;
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) return true;
        }
        return false;
    }
}
