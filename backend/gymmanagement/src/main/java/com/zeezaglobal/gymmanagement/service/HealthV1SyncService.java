package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.*;
import com.zeezaglobal.gymmanagement.entity.*;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.*;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthV1SyncService {

    private final UserRepository userRepository;
    private final HealthDeviceService deviceService;
    private final HealthReconciliationEngine reconciliationEngine;
    private final HealthCanonicalRecordRepository canonicalRecordRepository;
    private final HealthRecordTombstoneRepository tombstoneRepository;
    private final HealthUserSyncStateRepository userSyncStateRepository;
    private final DailyHealthRecordRepository dailyHealthRecordRepository;
    private final WorkoutRecordRepository workoutRecordRepository;
    private final PointsSyncService pointsSyncService;

    @Transactional
    public HealthV1SyncResponse sync(HealthV1SyncRequest request, UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String deviceId = request.deviceId() != null ? request.deviceId() : "unknown-device";
        deviceService.updateLastSync(deviceId);

        Set<LocalDate> affectedDates = new HashSet<>();
        int uploadedCount = 0;

        // 1. Ingest Raw Records
        if (request.rawRecords() != null && !request.rawRecords().isEmpty()) {
            uploadedCount += reconciliationEngine.ingestRawRecords(user, deviceId, request.rawRecords());
            for (HealthRawRecordDto dto : request.rawRecords()) {
                if (dto.startTime() != null) {
                    affectedDates.add(dto.startTime().toLocalDate());
                }
            }
        }

        // 2. Ingest Daily Records as Raw Records if provided (ensures backward and multi-device parity)
        if (request.dailyRecords() != null && !request.dailyRecords().isEmpty()) {
            List<HealthRawRecordDto> convertedRaw = new ArrayList<>();
            for (DailyHealthSyncDto d : request.dailyRecords()) {
                if (d.recordDate() == null) continue;
                LocalDate date = d.recordDate();
                affectedDates.add(date);

                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(23, 59, 59);
                String sourceSystem = d.sourceDevice() != null ? d.sourceDevice() : "MOBILE_APP";

                if (d.steps() != null && d.steps() > 0) {
                    convertedRaw.add(new HealthRawRecordDto(
                            "steps-" + date + "-" + sourceSystem,
                            "STEPS", startOfDay, endOfDay, d.steps().doubleValue(), "count",
                            null, null, sourceSystem.contains("WATCH") ? "WEARABLE" : "PHONE",
                            null, null, sourceSystem, null, null, 0.85));
                }
                if (d.activeCalories() != null && d.activeCalories() > 0) {
                    convertedRaw.add(new HealthRawRecordDto(
                            "cal-active-" + date + "-" + sourceSystem,
                            "CALORIES_ACTIVE", startOfDay, endOfDay, d.activeCalories(), "kcal",
                            null, null, sourceSystem.contains("WATCH") ? "WEARABLE" : "PHONE",
                            null, null, sourceSystem, null, null, 0.85));
                }
                if (d.totalCalories() != null && d.totalCalories() > 0) {
                    convertedRaw.add(new HealthRawRecordDto(
                            "cal-total-" + date + "-" + sourceSystem,
                            "CALORIES_TOTAL", startOfDay, endOfDay, d.totalCalories(), "kcal",
                            null, null, sourceSystem.contains("WATCH") ? "WEARABLE" : "PHONE",
                            null, null, sourceSystem, null, null, 0.85));
                }
                if (d.latestHeartRateBpm() != null && d.latestHeartRateBpm() > 0) {
                    convertedRaw.add(new HealthRawRecordDto(
                            "hr-" + date + "-" + sourceSystem,
                            "HEART_RATE", startOfDay, endOfDay, d.latestHeartRateBpm().doubleValue(), "bpm",
                            d.heartRateSamplesJson(), null, sourceSystem.contains("WATCH") ? "WEARABLE" : "PHONE",
                            null, null, sourceSystem, null, null, 0.85));
                }
                if (d.sleepDurationMinutes() != null && d.sleepDurationMinutes() > 0) {
                    LocalDateTime sStart = d.sleepStartTime() != null ? d.sleepStartTime() : startOfDay;
                    LocalDateTime sEnd = d.sleepEndTime() != null ? d.sleepEndTime() : endOfDay;
                    convertedRaw.add(new HealthRawRecordDto(
                            "sleep-" + date + "-" + sourceSystem,
                            "SLEEP", sStart, sEnd, d.sleepDurationMinutes().doubleValue(), "minutes",
                            d.sleepStagesJson(), null, sourceSystem.contains("WATCH") ? "WEARABLE" : "PHONE",
                            null, null, sourceSystem, null, null, 0.85));
                }
                if (d.weightKg() != null && d.weightKg() > 0) {
                    convertedRaw.add(new HealthRawRecordDto(
                            "weight-" + date + "-" + sourceSystem,
                            "WEIGHT", startOfDay, endOfDay, d.weightKg(), "kg",
                            null, null, "SCALE", null, null, sourceSystem, null, null, 0.90));
                }
            }
            if (!convertedRaw.isEmpty()) {
                uploadedCount += reconciliationEngine.ingestRawRecords(user, deviceId, convertedRaw);
            }
        }

        // 3. Process Deletions / Tombstones
        if (request.deletedRecordIds() != null && !request.deletedRecordIds().isEmpty()) {
            reconciliationEngine.processDeletions(user, deviceId, request.deletedRecordIds());
        }

        // 4. Ingest Workouts
        LocalDateTime now = LocalDateTime.now();
        if (request.workouts() != null) {
            for (WorkoutSyncDto wDto : request.workouts()) {
                if (wDto.externalId() == null || wDto.externalId().isBlank()) continue;

                Optional<WorkoutRecord> optW = workoutRecordRepository
                        .findByUserIdAndExternalId(user.getId(), wDto.externalId());

                if (optW.isEmpty()) {
                    List<WorkoutRecord> overlapping = workoutRecordRepository
                            .findByUserIdAndStartTimeBetweenOrderByStartTimeDesc(
                                    user.getId(),
                                    (wDto.startTime() != null ? wDto.startTime() : now).minusMinutes(45),
                                    (wDto.startTime() != null ? wDto.startTime() : now).plusMinutes(45)
                            );
                    if (!overlapping.isEmpty()) {
                        optW = Optional.of(overlapping.get(0));
                    }
                }

                WorkoutRecord wr = optW.orElseGet(() -> {
                    WorkoutRecord r = new WorkoutRecord();
                    r.setUser(user);
                    r.setExternalId(wDto.externalId());
                    return r;
                });

                wr.setTitle(wDto.title() != null ? wDto.title() : "Workout");
                wr.setStartTime(wDto.startTime() != null ? wDto.startTime() : now);
                wr.setEndTime(wDto.endTime() != null ? wDto.endTime() : now);
                wr.setDurationMinutes(wDto.durationMinutes() != null ? wDto.durationMinutes() : 0);
                wr.setCaloriesBurned(wDto.caloriesBurned());
                wr.setSetCount(wDto.setCount());
                wr.setTotalReps(wDto.totalReps());
                wr.setTotalVolumeKg(wDto.totalVolumeKg());
                wr.setSourceDevice(wDto.sourceDevice() != null ? wDto.sourceDevice() : deviceId);
                wr.setUpdatedAt(now);
                workoutRecordRepository.save(wr);
            }
        }

        // 5. Trigger Canonical Reconciliation for affected dates
        List<HealthCanonicalRecord> reconciled = reconciliationEngine.reconcileUserMetrics(user, affectedDates);

        // 6. Fetch deltas since lastSyncVersion
        long cursor = request.lastSyncVersion() != null ? request.lastSyncVersion() : 0L;

        List<HealthCanonicalRecord> deltaCanonicals = canonicalRecordRepository
                .findByUserIdAndServerVersionGreaterThanOrderByServerVersionAsc(user.getId(), cursor);

        List<HealthRecordTombstone> deltaTombstones = tombstoneRepository
                .findByUserIdAndServerVersionGreaterThanOrderByServerVersionAsc(user.getId(), cursor);

        long currentVersion = userSyncStateRepository.findById(user.getId())
                .map(HealthUserSyncState::getLastVersion)
                .orElse(cursor);

        // Map to DTOs
        List<HealthCanonicalRecordDto> canonicalDtos = deltaCanonicals.stream().map(c -> new HealthCanonicalRecordDto(
                c.getId(),
                c.getMetricType(),
                c.getStartTime(),
                c.getEndTime(),
                c.getValue(),
                c.getUnit(),
                c.getPayloadJson(),
                c.getSelectedSourceSystem(),
                c.getSelectedDeviceId(),
                c.getConfidence(),
                c.getServerVersion(),
                c.isDeleted()
        )).toList();

        List<HealthTombstoneDto> tombstoneDtos = deltaTombstones.stream().map(t -> new HealthTombstoneDto(
                t.getCanonicalRecordId(),
                t.getMetricType(),
                t.getDeletedAt(),
                t.getServerVersion()
        )).toList();

        // Also fetch daily health records (last 30 days) and workouts for convenient mobile ingestion
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<DailyHealthSyncDto> dailyDtos = dailyHealthRecordRepository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(user.getId(), thirtyDaysAgo, LocalDate.now())
                .stream().map(r -> new DailyHealthSyncDto(
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
                )).toList();

        List<WorkoutSyncDto> workoutDtos = workoutRecordRepository
                .findByUserIdOrderByStartTimeDesc(user.getId())
                .stream().map(w -> new WorkoutSyncDto(
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
                )).toList();

        // 7. Synchronize points
        PointsSyncResponse pointsResp;
        if (request.pointsTransactions() != null && !request.pointsTransactions().isEmpty()) {
            pointsResp = pointsSyncService.syncPoints(user, deviceId, request.pointsTransactions());
        } else {
            pointsResp = pointsSyncService.getPoints(user);
        }

        log.info("Health V1 sync completed for user {} device {}: ingested raw {}, reconciled {}, returning {} canonicals, {} tombstones, pointsBalance={}, serverVersion={}",
                user.getId(), deviceId, uploadedCount, reconciled.size(), canonicalDtos.size(), tombstoneDtos.size(), pointsResp.balance(), currentVersion);

        return new HealthV1SyncResponse(
                currentVersion,
                now,
                uploadedCount,
                reconciled.size(),
                canonicalDtos,
                tombstoneDtos,
                dailyDtos,
                workoutDtos,
                pointsResp.balance(),
                pointsResp.transactions()
        );
    }
}
