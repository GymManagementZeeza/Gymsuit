package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.HealthRawRecordDto;
import com.zeezaglobal.gymmanagement.entity.*;
import com.zeezaglobal.gymmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthReconciliationEngineTest {

    @Mock
    private HealthRawRecordRepository rawRecordRepository;
    @Mock
    private HealthCanonicalRecordRepository canonicalRecordRepository;
    @Mock
    private HealthRecordTombstoneRepository tombstoneRepository;
    @Mock
    private HealthUserSyncStateRepository userSyncStateRepository;
    @Mock
    private HealthSourcePreferenceRepository preferenceRepository;
    @Mock
    private DailyHealthRecordRepository dailyHealthRecordRepository;

    @Spy
    private HealthReconciliationConfig reconciliationConfig = new HealthReconciliationConfig();

    @InjectMocks
    private HealthReconciliationEngine reconciliationEngine;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("athul.6560@gmail.com");
    }

    @Test
    void testComputeDedupHashIsDeterministic() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 4, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 4, 9, 0);

        String hash1 = reconciliationEngine.computeDedupHash(1L, "STEPS", start, end, 5000.0, "APPLE_HEALTH", "rec-1");
        String hash2 = reconciliationEngine.computeDedupHash(1L, "STEPS", start, end, 5000.0, "APPLE_HEALTH", "rec-1");
        String hashDiff = reconciliationEngine.computeDedupHash(1L, "STEPS", start, end, 5500.0, "APPLE_HEALTH", "rec-1");

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hashDiff);
    }

    @Test
    void testSourcePriorityWearableHigherThanPhone() {
        double watchScore = reconciliationConfig.calculateSourceScore("STEPS", "WEARABLE", "SAMSUNG", 0.9);
        double phoneScore = reconciliationConfig.calculateSourceScore("STEPS", "PHONE", "APPLE", 0.9);

        assertTrue(watchScore > phoneScore, "Wearable should have higher priority score than Phone");
    }

    @Test
    void testReconcileMultiDeviceStepsSelectsWatchOverPhone() {
        LocalDate date = LocalDate.of(2026, 10, 4);
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(23, 59, 59);

        // iPhone record: 12,000 steps
        HealthRawRecord iphoneRecord = new HealthRawRecord();
        iphoneRecord.setId(101L);
        iphoneRecord.setUser(testUser);
        iphoneRecord.setMetricType("STEPS");
        iphoneRecord.setStartTime(dayStart);
        iphoneRecord.setEndTime(dayEnd);
        iphoneRecord.setValue(12000.0);
        iphoneRecord.setSourceDeviceType("PHONE");
        iphoneRecord.setSourceManufacturer("APPLE");
        iphoneRecord.setSourceSystem("APPLE_HEALTH");
        iphoneRecord.setConfidence(0.8);

        // Samsung Galaxy Watch record: 14,500 steps
        HealthRawRecord watchRecord = new HealthRawRecord();
        watchRecord.setId(102L);
        watchRecord.setUser(testUser);
        watchRecord.setMetricType("STEPS");
        watchRecord.setStartTime(dayStart);
        watchRecord.setEndTime(dayEnd);
        watchRecord.setValue(14500.0);
        watchRecord.setSourceDeviceType("WEARABLE");
        watchRecord.setSourceManufacturer("SAMSUNG");
        watchRecord.setSourceSystem("SAMSUNG_HEALTH");
        watchRecord.setConfidence(0.95);

        when(rawRecordRepository.findByUserIdAndStartTimeBetweenOrderByStartTimeAsc(eq(1L), any(), any()))
                .thenReturn(List.of(iphoneRecord, watchRecord));

        when(dailyHealthRecordRepository.findByUserIdAndRecordDate(1L, date))
                .thenReturn(Optional.of(new DailyHealthRecord()));

        HealthUserSyncState syncState = new HealthUserSyncState(1L, 5L, LocalDateTime.now());
        when(userSyncStateRepository.findById(1L)).thenReturn(Optional.of(syncState));

        when(canonicalRecordRepository.findByUserIdAndMetricTypeAndStartTimeAndEndTime(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        when(canonicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<HealthCanonicalRecord> canonicals = reconciliationEngine.reconcileUserMetrics(testUser, Set.of(date));

        assertFalse(canonicals.isEmpty());
        HealthCanonicalRecord stepsCanonical = canonicals.stream()
                .filter(c -> "STEPS".equals(c.getMetricType()))
                .findFirst().orElse(null);

        assertNotNull(stepsCanonical);
        assertEquals(14500.0, stepsCanonical.getValue(), "Samsung Watch step count should be preferred");
        assertEquals("SAMSUNG_HEALTH", stepsCanonical.getSelectedSourceSystem());
    }

    @Test
    void testDeletionsCreateTombstones() {
        HealthCanonicalRecord record = new HealthCanonicalRecord();
        record.setId("can-uuid-123");
        record.setUser(testUser);
        record.setMetricType("STEPS");
        record.setServerVersion(10L);

        when(canonicalRecordRepository.findById("can-uuid-123")).thenReturn(Optional.of(record));

        HealthUserSyncState syncState = new HealthUserSyncState(1L, 10L, LocalDateTime.now());
        when(userSyncStateRepository.findById(1L)).thenReturn(Optional.of(syncState));

        reconciliationEngine.processDeletions(testUser, "device-ios", List.of("can-uuid-123"));

        assertTrue(record.isDeleted());
        verify(tombstoneRepository, times(1)).save(any(HealthRecordTombstone.class));
    }
}
