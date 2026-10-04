package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.DailyHealthSyncDto;
import com.zeezaglobal.gymmanagement.dto.HealthSyncRequest;
import com.zeezaglobal.gymmanagement.dto.HealthSyncResponse;
import com.zeezaglobal.gymmanagement.dto.WorkoutSyncDto;
import com.zeezaglobal.gymmanagement.entity.DailyHealthRecord;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.entity.WorkoutRecord;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.DailyHealthRecordRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.repository.WorkoutRecordRepository;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthSyncService {

    private final DailyHealthRecordRepository dailyHealthRecordRepository;
    private final WorkoutRecordRepository workoutRecordRepository;
    private final UserRepository userRepository;
    private final HealthReconciliationEngine reconciliationEngine;

    @Transactional
    public HealthSyncResponse sync(HealthSyncRequest request, UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDateTime now = LocalDateTime.now();
        int uploadedDaily = 0;
        int uploadedWorkouts = 0;

        // 1. Ingest incoming daily health metrics
        if (request.dailyRecords() != null) {
            for (DailyHealthSyncDto dto : request.dailyRecords()) {
                if (dto.recordDate() == null) continue;

                Optional<DailyHealthRecord> opt = dailyHealthRecordRepository
                        .findByUserIdAndRecordDate(user.getId(), dto.recordDate());

                DailyHealthRecord record = opt.orElseGet(() -> {
                    DailyHealthRecord r = new DailyHealthRecord();
                    r.setUser(user);
                    r.setRecordDate(dto.recordDate());
                    return r;
                });

                // Smart merge: take data from the device with most amount of data (larger data wins)
                double existingDailyScore = calculateDailyDataScore(
                        record.getSteps(),
                        record.getActiveCalories(),
                        record.getDistanceMeters(),
                        record.getSleepDurationMinutes(),
                        record.getHeartRateSamplesJson(),
                        record.getSleepStagesJson()
                );

                double incomingDailyScore = calculateDailyDataScore(
                        dto.steps(),
                        dto.activeCalories(),
                        dto.distanceMeters(),
                        dto.sleepDurationMinutes(),
                        dto.heartRateSamplesJson(),
                        dto.sleepStagesJson()
                );

                if (dto.steps() != null && (record.getSteps() == null || dto.steps() >= record.getSteps())) {
                    record.setSteps(dto.steps());
                }
                if (dto.activeCalories() != null && (record.getActiveCalories() == null || dto.activeCalories() >= record.getActiveCalories())) {
                    record.setActiveCalories(dto.activeCalories());
                }
                if (dto.totalCalories() != null && (record.getTotalCalories() == null || dto.totalCalories() >= record.getTotalCalories())) {
                    record.setTotalCalories(dto.totalCalories());
                }
                if (dto.distanceMeters() != null && (record.getDistanceMeters() == null || dto.distanceMeters() >= record.getDistanceMeters())) {
                    record.setDistanceMeters(dto.distanceMeters());
                }
                if (dto.latestHeartRateBpm() != null && (record.getLatestHeartRateBpm() == null || incomingDailyScore >= existingDailyScore)) {
                    record.setLatestHeartRateBpm(dto.latestHeartRateBpm());
                }
                if (dto.restingHeartRateBpm() != null) {
                    record.setRestingHeartRateBpm(dto.restingHeartRateBpm());
                }
                if (dto.minHeartRateBpm() != null) {
                    record.setMinHeartRateBpm(record.getMinHeartRateBpm() == null ? dto.minHeartRateBpm() : Math.min(record.getMinHeartRateBpm(), dto.minHeartRateBpm()));
                }
                if (dto.maxHeartRateBpm() != null) {
                    record.setMaxHeartRateBpm(record.getMaxHeartRateBpm() == null ? dto.maxHeartRateBpm() : Math.max(record.getMaxHeartRateBpm(), dto.maxHeartRateBpm()));
                }
                if (dto.heartRateSamplesJson() != null && !dto.heartRateSamplesJson().isBlank()) {
                    if (record.getHeartRateSamplesJson() == null || dto.heartRateSamplesJson().length() >= record.getHeartRateSamplesJson().length()) {
                        record.setHeartRateSamplesJson(dto.heartRateSamplesJson());
                    }
                }
                if (dto.sleepDurationMinutes() != null && dto.sleepDurationMinutes() > 0) {
                    // Only overwrite sleep if incoming duration is larger or existing is empty
                    if (record.getSleepDurationMinutes() == null || dto.sleepDurationMinutes() >= record.getSleepDurationMinutes()) {
                        record.setSleepDurationMinutes(dto.sleepDurationMinutes());
                        if (dto.sleepStartTime() != null) record.setSleepStartTime(dto.sleepStartTime());
                        if (dto.sleepEndTime() != null) record.setSleepEndTime(dto.sleepEndTime());
                        if (dto.sleepStagesJson() != null && !dto.sleepStagesJson().isBlank()) {
                            record.setSleepStagesJson(dto.sleepStagesJson());
                        }
                    }
                }
                if (dto.weightKg() != null && dto.weightKg() > 0) {
                    record.setWeightKg(dto.weightKg());
                }

                // Attribute source device to the device that contributed the larger data volume
                if (record.getSourceDevice() == null || incomingDailyScore >= existingDailyScore) {
                    String source = dto.sourceDevice() != null ? dto.sourceDevice() : request.clientDevice();
                    if (source != null) {
                        record.setSourceDevice(source);
                    }
                }
                record.setUpdatedAt(now);

                dailyHealthRecordRepository.save(record);
                uploadedDaily++;
            }

            // Also feed and reconcile into canonical multi-device engine
            try {
                java.util.Set<LocalDate> affectedDates = new java.util.HashSet<>();
                List<com.zeezaglobal.gymmanagement.dto.HealthRawRecordDto> rawDtos = new ArrayList<>();
                for (DailyHealthSyncDto d : request.dailyRecords()) {
                    if (d.recordDate() == null) continue;
                    LocalDate date = d.recordDate();
                    affectedDates.add(date);
                    LocalDateTime startOfDay = date.atStartOfDay();
                    LocalDateTime endOfDay = date.atTime(23, 59, 59);
                    String src = d.sourceDevice() != null ? d.sourceDevice() : (request.clientDevice() != null ? request.clientDevice() : "MOBILE_APP");

                    if (d.steps() != null && d.steps() > 0) {
                        rawDtos.add(new com.zeezaglobal.gymmanagement.dto.HealthRawRecordDto(
                                "steps-" + date + "-" + src,
                                "STEPS", startOfDay, endOfDay, d.steps().doubleValue(), "count",
                                null, null, src.contains("WATCH") ? "WEARABLE" : "PHONE",
                                null, null, src, null, null, 0.85));
                    }
                    if (d.activeCalories() != null && d.activeCalories() > 0) {
                        rawDtos.add(new com.zeezaglobal.gymmanagement.dto.HealthRawRecordDto(
                                "cal-active-" + date + "-" + src,
                                "CALORIES_ACTIVE", startOfDay, endOfDay, d.activeCalories(), "kcal",
                                null, null, src.contains("WATCH") ? "WEARABLE" : "PHONE",
                                null, null, src, null, null, 0.85));
                    }
                    if (d.totalCalories() != null && d.totalCalories() > 0) {
                        rawDtos.add(new com.zeezaglobal.gymmanagement.dto.HealthRawRecordDto(
                                "cal-total-" + date + "-" + src,
                                "CALORIES_TOTAL", startOfDay, endOfDay, d.totalCalories(), "kcal",
                                null, null, src.contains("WATCH") ? "WEARABLE" : "PHONE",
                                null, null, src, null, null, 0.85));
                    }
                    if (d.latestHeartRateBpm() != null && d.latestHeartRateBpm() > 0) {
                        rawDtos.add(new com.zeezaglobal.gymmanagement.dto.HealthRawRecordDto(
                                "hr-" + date + "-" + src,
                                "HEART_RATE", startOfDay, endOfDay, d.latestHeartRateBpm().doubleValue(), "bpm",
                                d.heartRateSamplesJson(), null, src.contains("WATCH") ? "WEARABLE" : "PHONE",
                                null, null, src, null, null, 0.85));
                    }
                    if (d.sleepDurationMinutes() != null && d.sleepDurationMinutes() > 0) {
                        rawDtos.add(new com.zeezaglobal.gymmanagement.dto.HealthRawRecordDto(
                                "sleep-" + date + "-" + src,
                                "SLEEP", d.sleepStartTime() != null ? d.sleepStartTime() : startOfDay,
                                d.sleepEndTime() != null ? d.sleepEndTime() : endOfDay,
                                d.sleepDurationMinutes().doubleValue(), "minutes",
                                d.sleepStagesJson(), null, src.contains("WATCH") ? "WEARABLE" : "PHONE",
                                null, null, src, null, null, 0.85));
                    }
                }
                if (!rawDtos.isEmpty()) {
                    reconciliationEngine.ingestRawRecords(user, request.clientDevice() != null ? request.clientDevice() : "MOBILE_APP", rawDtos);
                    reconciliationEngine.reconcileUserMetrics(user, affectedDates);
                }
            } catch (Exception e) {
                log.warn("Canonical engine sync caught non-fatal exception during legacy sync: {}", e.getMessage());
            }
        }

        // 2. Ingest incoming workout logs with larger-data conflict resolution
        if (request.workouts() != null) {
            for (WorkoutSyncDto wDto : request.workouts()) {
                if (wDto.externalId() == null || wDto.externalId().isBlank()) continue;

                LocalDateTime wStart = wDto.startTime() != null ? wDto.startTime() : now;
                double incomingScore = calculateWorkoutDtoScore(wDto);

                // Find by external ID first
                Optional<WorkoutRecord> optW = workoutRecordRepository
                        .findByUserIdAndExternalId(user.getId(), wDto.externalId());

                // If not found by external ID, check for overlapping workouts within 45 minutes
                if (optW.isEmpty()) {
                    List<WorkoutRecord> overlapping = workoutRecordRepository
                            .findByUserIdAndStartTimeBetweenOrderByStartTimeDesc(
                                    user.getId(),
                                    wStart.minusMinutes(45),
                                    wStart.plusMinutes(45)
                            );
                    if (!overlapping.isEmpty()) {
                        optW = Optional.of(overlapping.get(0));
                    }
                }

                if (optW.isPresent()) {
                    WorkoutRecord existing = optW.get();
                    double existingScore = calculateWorkoutScore(existing);
                    // If existing workout has more data than incoming, retain the larger existing data
                    if (existingScore > incomingScore) {
                        log.info("Retaining existing larger workout {} (score: {}) over incoming (score: {})",
                                existing.getId(), existingScore, incomingScore);
                        continue;
                    }
                    // Incoming has equal or larger data: update existing record
                    existing.setTitle(wDto.title() != null ? wDto.title() : existing.getTitle());
                    existing.setStartTime(wDto.startTime() != null ? wDto.startTime() : existing.getStartTime());
                    existing.setEndTime(wDto.endTime() != null ? wDto.endTime() : existing.getEndTime());
                    existing.setDurationMinutes(wDto.durationMinutes() != null ? wDto.durationMinutes() : existing.getDurationMinutes());
                    existing.setCaloriesBurned(wDto.caloriesBurned() != null ? wDto.caloriesBurned() : existing.getCaloriesBurned());
                    existing.setSetCount(wDto.setCount() != null ? wDto.setCount() : existing.getSetCount());
                    existing.setTotalReps(wDto.totalReps() != null ? wDto.totalReps() : existing.getTotalReps());
                    existing.setTotalVolumeKg(wDto.totalVolumeKg() != null ? wDto.totalVolumeKg() : existing.getTotalVolumeKg());
                    existing.setSourceDevice(wDto.sourceDevice() != null ? wDto.sourceDevice() : request.clientDevice());
                    existing.setUpdatedAt(now);

                    workoutRecordRepository.save(existing);
                    uploadedWorkouts++;
                } else {
                    // New workout record
                    WorkoutRecord wr = new WorkoutRecord();
                    wr.setUser(user);
                    wr.setExternalId(wDto.externalId());
                    wr.setTitle(wDto.title() != null ? wDto.title() : "Workout");
                    wr.setStartTime(wDto.startTime() != null ? wDto.startTime() : now);
                    wr.setEndTime(wDto.endTime() != null ? wDto.endTime() : now);
                    wr.setDurationMinutes(wDto.durationMinutes() != null ? wDto.durationMinutes() : 0);
                    wr.setCaloriesBurned(wDto.caloriesBurned());
                    wr.setSetCount(wDto.setCount());
                    wr.setTotalReps(wDto.totalReps());
                    wr.setTotalVolumeKg(wDto.totalVolumeKg());
                    wr.setSourceDevice(wDto.sourceDevice() != null ? wDto.sourceDevice() : request.clientDevice());
                    wr.setUpdatedAt(now);

                    workoutRecordRepository.save(wr);
                    uploadedWorkouts++;
                }
            }
        }

        // 3. Query records to sync down to client
        List<DailyHealthRecord> remoteDaily;
        List<WorkoutRecord> remoteWorkouts;

        if (request.lastSyncTime() != null) {
            remoteDaily = dailyHealthRecordRepository.findUpdatedSince(user.getId(), request.lastSyncTime());
            remoteWorkouts = workoutRecordRepository.findUpdatedSince(user.getId(), request.lastSyncTime());
        } else {
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            remoteDaily = dailyHealthRecordRepository.findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
                    user.getId(), thirtyDaysAgo, LocalDate.now());
            remoteWorkouts = workoutRecordRepository.findByUserIdOrderByStartTimeDesc(user.getId());
        }

        List<DailyHealthSyncDto> remoteDailyDtos = remoteDaily.stream().map(this::toDto).toList();
        List<WorkoutSyncDto> remoteWorkoutDtos = remoteWorkouts.stream().map(this::toDto).toList();

        log.info("Health sync for user {}: uploaded {} daily, {} workouts; returning {} daily, {} workouts",
                user.getId(), uploadedDaily, uploadedWorkouts, remoteDailyDtos.size(), remoteWorkoutDtos.size());

        return new HealthSyncResponse(now, uploadedDaily, uploadedWorkouts, remoteDailyDtos, remoteWorkoutDtos);
    }

    @Transactional(readOnly = true)
    public List<DailyHealthSyncDto> getRecords(LocalDate startDate, LocalDate endDate, UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return dailyHealthRecordRepository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(user.getId(), start, end)
                .stream().map(this::toDto).toList();
    }

    private DailyHealthSyncDto toDto(DailyHealthRecord r) {
        return new DailyHealthSyncDto(
                r.getRecordDate(),
                r.getSteps(),
                r.getActiveCalories(),
                r.getTotalCalories(),
                r.getDistanceMeters(),
                r.getLatestHeartRateBpm(),
                r.getRestingHeartRateBpm(),
                r.getMinHeartRateBpm(),
                r.getMaxHeartRateBpm(),
                r.getHeartRateSamplesJson(),
                r.getSleepDurationMinutes(),
                r.getSleepStartTime(),
                r.getSleepEndTime(),
                r.getSleepStagesJson(),
                r.getWeightKg(),
                r.getSourceDevice(),
                r.getUpdatedAt()
        );
    }

    private WorkoutSyncDto toDto(WorkoutRecord w) {
        return new WorkoutSyncDto(
                w.getExternalId(),
                w.getTitle(),
                w.getStartTime(),
                w.getEndTime(),
                w.getDurationMinutes(),
                w.getCaloriesBurned(),
                w.getSetCount(),
                w.getTotalReps(),
                w.getTotalVolumeKg(),
                w.getSourceDevice(),
                w.getUpdatedAt()
        );
    }

    private double calculateDailyDataScore(Long steps, Double activeCal, Double distance,
                                          Long sleepMins, String hrJson, String sleepStagesJson) {
        double score = 0.0;
        if (steps != null) score += steps;
        if (activeCal != null) score += activeCal * 10.0;
        if (distance != null) score += distance;
        if (sleepMins != null) score += sleepMins * 20.0;
        if (hrJson != null && !hrJson.isBlank()) score += hrJson.length();
        if (sleepStagesJson != null && !sleepStagesJson.isBlank()) score += sleepStagesJson.length();
        return score;
    }

    private double calculateWorkoutScore(WorkoutRecord w) {
        double score = 0.0;
        if (w.getTotalVolumeKg() != null) score += w.getTotalVolumeKg();
        if (w.getTotalReps() != null) score += w.getTotalReps() * 10.0;
        if (w.getDurationMinutes() != null) score += w.getDurationMinutes() * 5.0;
        if (w.getCaloriesBurned() != null) score += w.getCaloriesBurned() * 2.0;
        if (w.getSetCount() != null) score += w.getSetCount() * 15.0;
        return score;
    }

    private double calculateWorkoutDtoScore(WorkoutSyncDto w) {
        double score = 0.0;
        if (w.totalVolumeKg() != null) score += w.totalVolumeKg();
        if (w.totalReps() != null) score += w.totalReps() * 10.0;
        if (w.durationMinutes() != null) score += w.durationMinutes() * 5.0;
        if (w.caloriesBurned() != null) score += w.caloriesBurned() * 2.0;
        if (w.setCount() != null) score += w.setCount() * 15.0;
        return score;
    }
}
