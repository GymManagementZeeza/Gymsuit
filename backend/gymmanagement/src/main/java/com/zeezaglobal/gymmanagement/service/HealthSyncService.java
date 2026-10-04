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

                // Smart merge: retain maximum/most accurate data
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
                if (dto.latestHeartRateBpm() != null) {
                    record.setLatestHeartRateBpm(dto.latestHeartRateBpm());
                }
                if (dto.restingHeartRateBpm() != null) {
                    record.setRestingHeartRateBpm(dto.restingHeartRateBpm());
                }
                if (dto.minHeartRateBpm() != null) {
                    record.setMinHeartRateBpm(dto.minHeartRateBpm());
                }
                if (dto.maxHeartRateBpm() != null) {
                    record.setMaxHeartRateBpm(dto.maxHeartRateBpm());
                }
                if (dto.heartRateSamplesJson() != null && !dto.heartRateSamplesJson().isBlank()) {
                    record.setHeartRateSamplesJson(dto.heartRateSamplesJson());
                }
                if (dto.sleepDurationMinutes() != null && dto.sleepDurationMinutes() > 0) {
                    record.setSleepDurationMinutes(dto.sleepDurationMinutes());
                    if (dto.sleepStartTime() != null) record.setSleepStartTime(dto.sleepStartTime());
                    if (dto.sleepEndTime() != null) record.setSleepEndTime(dto.sleepEndTime());
                    if (dto.sleepStagesJson() != null && !dto.sleepStagesJson().isBlank()) {
                        record.setSleepStagesJson(dto.sleepStagesJson());
                    }
                }
                if (dto.weightKg() != null && dto.weightKg() > 0) {
                    record.setWeightKg(dto.weightKg());
                }

                String source = dto.sourceDevice() != null ? dto.sourceDevice() : request.clientDevice();
                if (source != null) {
                    record.setSourceDevice(source);
                }
                record.setUpdatedAt(now);

                dailyHealthRecordRepository.save(record);
                uploadedDaily++;
            }
        }

        // 2. Ingest incoming workout logs
        if (request.workouts() != null) {
            for (WorkoutSyncDto wDto : request.workouts()) {
                if (wDto.externalId() == null || wDto.externalId().isBlank()) continue;

                Optional<WorkoutRecord> optW = workoutRecordRepository
                        .findByUserIdAndExternalId(user.getId(), wDto.externalId());

                WorkoutRecord wr = optW.orElseGet(() -> {
                    WorkoutRecord w = new WorkoutRecord();
                    w.setUser(user);
                    w.setExternalId(wDto.externalId());
                    return w;
                });

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
}
