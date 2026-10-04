package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.HealthCanonicalRecordDto;
import com.zeezaglobal.gymmanagement.dto.HealthRawRecordDto;
import com.zeezaglobal.gymmanagement.dto.HealthTombstoneDto;
import com.zeezaglobal.gymmanagement.entity.*;
import com.zeezaglobal.gymmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthReconciliationEngine {

    private final HealthRawRecordRepository rawRecordRepository;
    private final HealthCanonicalRecordRepository canonicalRecordRepository;
    private final HealthRecordTombstoneRepository tombstoneRepository;
    private final HealthUserSyncStateRepository userSyncStateRepository;
    private final HealthSourcePreferenceRepository preferenceRepository;
    private final DailyHealthRecordRepository dailyHealthRecordRepository;
    private final HealthReconciliationConfig reconciliationConfig;

    public String computeDedupHash(Long userId, String metricType, LocalDateTime start, LocalDateTime end,
                                   Double value, String sourceSystem, String sourceRecordId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String rawKey = userId + ":" +
                    (metricType != null ? metricType.toUpperCase() : "") + ":" +
                    (start != null ? start.toString() : "") + ":" +
                    (end != null ? end.toString() : "") + ":" +
                    (value != null ? String.format(Locale.ROOT, "%.4f", value) : "0") + ":" +
                    (sourceSystem != null ? sourceSystem.toUpperCase() : "") + ":" +
                    (sourceRecordId != null ? sourceRecordId : "");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    @Transactional
    public Long nextServerVersion(Long userId) {
        HealthUserSyncState state = userSyncStateRepository.findById(userId).orElseGet(() -> {
            HealthUserSyncState s = new HealthUserSyncState();
            s.setUserId(userId);
            s.setLastVersion(0L);
            return s;
        });
        long next = state.getLastVersion() + 1;
        state.setLastVersion(next);
        state.setLastReconciledAt(LocalDateTime.now());
        userSyncStateRepository.save(state);
        return next;
    }

    @Transactional
    public int ingestRawRecords(User user, String deviceId, List<HealthRawRecordDto> rawDtos) {
        if (rawDtos == null || rawDtos.isEmpty()) return 0;
        int count = 0;
        LocalDateTime now = LocalDateTime.now();

        for (HealthRawRecordDto dto : rawDtos) {
            if (dto.metricType() == null || dto.startTime() == null || dto.endTime() == null || dto.value() == null) {
                continue;
            }

            String hash = computeDedupHash(
                    user.getId(),
                    dto.metricType(),
                    dto.startTime(),
                    dto.endTime(),
                    dto.value(),
                    dto.sourceSystem(),
                    dto.sourceRecordId() != null ? dto.sourceRecordId() : dto.clientRecordId()
            );

            if (rawRecordRepository.existsByUserIdAndDedupHash(user.getId(), hash)) {
                continue; // Deduplicated
            }

            HealthRawRecord record = new HealthRawRecord();
            record.setUser(user);
            record.setDeviceId(deviceId);
            record.setMetricType(dto.metricType().toUpperCase());
            record.setStartTime(dto.startTime());
            record.setEndTime(dto.endTime());
            record.setValue(dto.value());
            record.setUnit(dto.unit());
            record.setPayloadJson(dto.payloadJson());
            record.setSourcePlatform(dto.sourcePlatform());
            record.setSourceDeviceType(dto.sourceDeviceType());
            record.setSourceManufacturer(dto.sourceManufacturer());
            record.setSourceModel(dto.sourceModel());
            record.setSourceSystem(dto.sourceSystem());
            record.setSourceAppId(dto.sourceAppId());
            record.setSourceRecordId(dto.sourceRecordId() != null ? dto.sourceRecordId() : dto.clientRecordId());
            record.setDedupHash(hash);
            record.setConfidence(dto.confidence() != null ? dto.confidence() : 0.85);
            record.setStatus("RAW");
            record.setCreatedAt(now);
            record.setClientSyncAt(now);

            rawRecordRepository.save(record);
            count++;
        }
        return count;
    }

    @Transactional
    public List<HealthCanonicalRecord> reconcileUserMetrics(User user, Set<LocalDate> datesToReconcile) {
        List<HealthCanonicalRecord> changedRecords = new ArrayList<>();
        if (datesToReconcile == null || datesToReconcile.isEmpty()) {
            return changedRecords;
        }

        for (LocalDate date : datesToReconcile) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(23, 59, 59);

            List<HealthRawRecord> rawRecords = rawRecordRepository
                    .findByUserIdAndStartTimeBetweenOrderByStartTimeAsc(user.getId(), dayStart, dayEnd);

            if (rawRecords.isEmpty()) {
                continue;
            }

            Map<String, List<HealthRawRecord>> byMetric = rawRecords.stream()
                    .collect(Collectors.groupingBy(HealthRawRecord::getMetricType));

            DailyHealthRecord dailyRecord = dailyHealthRecordRepository
                    .findByUserIdAndRecordDate(user.getId(), date)
                    .orElseGet(() -> {
                        DailyHealthRecord d = new DailyHealthRecord();
                        d.setUser(user);
                        d.setRecordDate(date);
                        return d;
                    });

            // 1. STEPS Reconciliation
            if (byMetric.containsKey("STEPS")) {
                HealthCanonicalRecord canonicalStep = reconcileSteps(user, date, byMetric.get("STEPS"));
                if (canonicalStep != null) {
                    changedRecords.add(canonicalStep);
                    dailyRecord.setSteps(canonicalStep.getValue().longValue());
                    dailyRecord.setSourceDevice(canonicalStep.getSelectedSourceSystem());
                }
            }

            // 2. HEART_RATE Reconciliation
            if (byMetric.containsKey("HEART_RATE")) {
                HealthCanonicalRecord canonicalHr = reconcileHeartRate(user, date, byMetric.get("HEART_RATE"), dailyRecord);
                if (canonicalHr != null) {
                    changedRecords.add(canonicalHr);
                }
            }

            // 3. SLEEP Reconciliation
            if (byMetric.containsKey("SLEEP")) {
                HealthCanonicalRecord canonicalSleep = reconcileSleep(user, date, byMetric.get("SLEEP"), dailyRecord);
                if (canonicalSleep != null) {
                    changedRecords.add(canonicalSleep);
                }
            }

            // 4. CALORIES Reconciliation
            if (byMetric.containsKey("CALORIES_ACTIVE") || byMetric.containsKey("CALORIES_TOTAL")) {
                List<HealthRawRecord> calActive = byMetric.getOrDefault("CALORIES_ACTIVE", Collections.emptyList());
                List<HealthRawRecord> calTotal = byMetric.getOrDefault("CALORIES_TOTAL", Collections.emptyList());
                reconcileCalories(user, date, calActive, calTotal, changedRecords, dailyRecord);
            }

            // 5. DISTANCE Reconciliation
            if (byMetric.containsKey("DISTANCE")) {
                HealthRawRecord bestDist = pickBestRawRecord("DISTANCE", byMetric.get("DISTANCE"));
                if (bestDist != null) {
                    HealthCanonicalRecord canDist = saveOrUpdateCanonical(
                            user, "DISTANCE", bestDist.getStartTime(), bestDist.getEndTime(),
                            bestDist.getValue(), bestDist.getUnit(), null, bestDist
                    );
                    changedRecords.add(canDist);
                    dailyRecord.setDistanceMeters(bestDist.getValue());
                }
            }

            // 6. WEIGHT Reconciliation
            if (byMetric.containsKey("WEIGHT")) {
                HealthRawRecord bestWeight = pickBestRawRecord("WEIGHT", byMetric.get("WEIGHT"));
                if (bestWeight != null) {
                    HealthCanonicalRecord canWeight = saveOrUpdateCanonical(
                            user, "WEIGHT", bestWeight.getStartTime(), bestWeight.getEndTime(),
                            bestWeight.getValue(), bestWeight.getUnit(), null, bestWeight
                    );
                    changedRecords.add(canWeight);
                    dailyRecord.setWeightKg(bestWeight.getValue());
                }
            }

            dailyRecord.setUpdatedAt(LocalDateTime.now());
            dailyHealthRecordRepository.save(dailyRecord);
        }

        return changedRecords;
    }

    private HealthCanonicalRecord reconcileSteps(User user, LocalDate date, List<HealthRawRecord> stepRecords) {
        if (stepRecords.isEmpty()) return null;

        // Group by source device/system
        Map<String, List<HealthRawRecord>> bySource = stepRecords.stream()
                .collect(Collectors.groupingBy(r -> r.getSourceSystem() != null ? r.getSourceSystem() : "UNKNOWN"));

        String bestSource = null;
        double bestScore = -1.0;
        double selectedTotal = 0.0;
        HealthRawRecord representativeRecord = null;

        for (Map.Entry<String, List<HealthRawRecord>> entry : bySource.entrySet()) {
            List<HealthRawRecord> records = entry.getValue();
            HealthRawRecord sample = records.get(0);
            double sourceScore = reconciliationConfig.calculateSourceScore(
                    "STEPS", sample.getSourceDeviceType(), sample.getSourceManufacturer(), sample.getConfidence());

            // Check if cumulative total is stored in 1 day-record or sum of disjoint chunks
            double sumSteps = records.stream().mapToDouble(HealthRawRecord::getValue).sum();
            double maxStep = records.stream().mapToDouble(HealthRawRecord::getValue).max().orElse(0.0);
            double candidateTotal = (records.size() == 1 || maxStep > sumSteps * 0.7) ? maxStep : sumSteps;

            // Combine source priority and total quantity
            double totalScore = sourceScore + (candidateTotal * 0.01);
            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestSource = entry.getKey();
                selectedTotal = candidateTotal;
                representativeRecord = sample;
            }
        }

        if (representativeRecord == null) return null;

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        return saveOrUpdateCanonical(user, "STEPS", start, end, selectedTotal, "count", null, representativeRecord);
    }

    private HealthCanonicalRecord reconcileHeartRate(User user, LocalDate date, List<HealthRawRecord> hrRecords, DailyHealthRecord dailyRecord) {
        if (hrRecords.isEmpty()) return null;

        // Pick highest quality source
        HealthRawRecord bestRecord = pickBestRawRecord("HEART_RATE", hrRecords);
        if (bestRecord == null) return null;

        dailyRecord.setLatestHeartRateBpm(bestRecord.getValue().intValue());

        // Find min and max
        int minBpm = hrRecords.stream().mapToInt(r -> r.getValue().intValue()).min().orElse(bestRecord.getValue().intValue());
        int maxBpm = hrRecords.stream().mapToInt(r -> r.getValue().intValue()).max().orElse(bestRecord.getValue().intValue());
        dailyRecord.setMinHeartRateBpm(minBpm);
        dailyRecord.setMaxHeartRateBpm(maxBpm);

        // Check if there is any payloadJson with full samples array
        for (HealthRawRecord r : hrRecords) {
            if (r.getPayloadJson() != null && r.getPayloadJson().contains("[")) {
                dailyRecord.setHeartRateSamplesJson(r.getPayloadJson());
                break;
            }
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);
        return saveOrUpdateCanonical(user, "HEART_RATE", start, end, bestRecord.getValue(), "bpm", dailyRecord.getHeartRateSamplesJson(), bestRecord);
    }

    private HealthCanonicalRecord reconcileSleep(User user, LocalDate date, List<HealthRawRecord> sleepRecords, DailyHealthRecord dailyRecord) {
        if (sleepRecords.isEmpty()) return null;

        HealthRawRecord best = pickBestRawRecord("SLEEP", sleepRecords);
        if (best == null) return null;

        dailyRecord.setSleepDurationMinutes(best.getValue().longValue());
        dailyRecord.setSleepStartTime(best.getStartTime());
        dailyRecord.setSleepEndTime(best.getEndTime());
        if (best.getPayloadJson() != null && !best.getPayloadJson().isBlank()) {
            dailyRecord.setSleepStagesJson(best.getPayloadJson());
        }

        return saveOrUpdateCanonical(user, "SLEEP", best.getStartTime(), best.getEndTime(), best.getValue(), "minutes", best.getPayloadJson(), best);
    }

    private void reconcileCalories(User user, LocalDate date, List<HealthRawRecord> activeList, List<HealthRawRecord> totalList,
                                   List<HealthCanonicalRecord> changedRecords, DailyHealthRecord dailyRecord) {
        if (!activeList.isEmpty()) {
            HealthRawRecord bestActive = pickBestRawRecord("CALORIES_ACTIVE", activeList);
            if (bestActive != null) {
                double total = activeList.size() == 1 ? bestActive.getValue() : activeList.stream().mapToDouble(HealthRawRecord::getValue).sum();
                HealthCanonicalRecord can = saveOrUpdateCanonical(
                        user, "CALORIES_ACTIVE", date.atStartOfDay(), date.atTime(23, 59, 59), total, "kcal", null, bestActive);
                changedRecords.add(can);
                dailyRecord.setActiveCalories(total);
            }
        }

        if (!totalList.isEmpty()) {
            HealthRawRecord bestTotal = pickBestRawRecord("CALORIES_TOTAL", totalList);
            if (bestTotal != null) {
                double total = totalList.size() == 1 ? bestTotal.getValue() : totalList.stream().mapToDouble(HealthRawRecord::getValue).sum();
                HealthCanonicalRecord can = saveOrUpdateCanonical(
                        user, "CALORIES_TOTAL", date.atStartOfDay(), date.atTime(23, 59, 59), total, "kcal", null, bestTotal);
                changedRecords.add(can);
                dailyRecord.setTotalCalories(total);
            }
        }
    }

    private HealthRawRecord pickBestRawRecord(String metricType, List<HealthRawRecord> records) {
        HealthRawRecord best = null;
        double bestScore = -1.0;
        for (HealthRawRecord r : records) {
            double score = reconciliationConfig.calculateSourceScore(
                    metricType, r.getSourceDeviceType(), r.getSourceManufacturer(), r.getConfidence());
            if (score > bestScore) {
                bestScore = score;
                best = r;
            }
        }
        return best;
    }

    private HealthCanonicalRecord saveOrUpdateCanonical(User user, String metricType, LocalDateTime start, LocalDateTime end,
                                                        Double value, String unit, String payloadJson, HealthRawRecord raw) {
        Optional<HealthCanonicalRecord> opt = canonicalRecordRepository
                .findByUserIdAndMetricTypeAndStartTimeAndEndTime(user.getId(), metricType, start, end);

        HealthCanonicalRecord record = opt.orElseGet(() -> {
            HealthCanonicalRecord r = new HealthCanonicalRecord();
            r.setId(UUID.randomUUID().toString());
            r.setUser(user);
            r.setMetricType(metricType);
            r.setStartTime(start);
            r.setEndTime(end);
            return r;
        });

        record.setValue(value);
        record.setUnit(unit);
        record.setPayloadJson(payloadJson);
        record.setSelectedRawRecordId(raw.getId());
        record.setSelectedDeviceId(raw.getDeviceId());
        record.setSelectedSourceSystem(raw.getSourceSystem());
        record.setConfidence(raw.getConfidence());
        record.setServerVersion(nextServerVersion(user.getId()));
        record.setReconciledAt(LocalDateTime.now());
        record.setDeleted(false);

        return canonicalRecordRepository.save(record);
    }

    @Transactional
    public void processDeletions(User user, String deviceId, List<String> deletedRecordIds) {
        if (deletedRecordIds == null || deletedRecordIds.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();

        for (String recordId : deletedRecordIds) {
            Optional<HealthCanonicalRecord> opt = canonicalRecordRepository.findById(recordId);
            if (opt.isPresent() && opt.get().getUser().getId().equals(user.getId())) {
                HealthCanonicalRecord can = opt.get();
                can.setDeleted(true);
                Long version = nextServerVersion(user.getId());
                can.setServerVersion(version);
                canonicalRecordRepository.save(can);

                HealthRecordTombstone tombstone = new HealthRecordTombstone();
                tombstone.setUser(user);
                tombstone.setCanonicalRecordId(can.getId());
                tombstone.setMetricType(can.getMetricType());
                tombstone.setDeletedAt(now);
                tombstone.setServerVersion(version);
                tombstone.setDeletedByDeviceId(deviceId);
                tombstoneRepository.save(tombstone);
            }
        }
    }
}
