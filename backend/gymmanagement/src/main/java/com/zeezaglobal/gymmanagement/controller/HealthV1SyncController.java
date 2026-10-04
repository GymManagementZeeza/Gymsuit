package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.DeviceRegistrationRequest;
import com.zeezaglobal.gymmanagement.dto.DeviceRegistrationResponse;
import com.zeezaglobal.gymmanagement.dto.HealthCanonicalRecordDto;
import com.zeezaglobal.gymmanagement.dto.HealthV1SyncRequest;
import com.zeezaglobal.gymmanagement.dto.HealthV1SyncResponse;
import com.zeezaglobal.gymmanagement.entity.HealthCanonicalRecord;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.HealthCanonicalRecordRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.service.HealthDeviceService;
import com.zeezaglobal.gymmanagement.service.HealthV1SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class HealthV1SyncController {

    private final HealthDeviceService deviceService;
    private final HealthV1SyncService syncService;
    private final UserRepository userRepository;
    private final HealthCanonicalRecordRepository canonicalRecordRepository;

    @PostMapping("/devices")
    public ResponseEntity<DeviceRegistrationResponse> registerDevice(
            @RequestBody DeviceRegistrationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        DeviceRegistrationResponse response = deviceService.registerDevice(user, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/health/sync")
    public ResponseEntity<HealthV1SyncResponse> syncHealthData(
            @RequestBody HealthV1SyncRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        HealthV1SyncResponse response = syncService.sync(request, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/canonical")
    public ResponseEntity<List<HealthCanonicalRecordDto>> getCanonicalRecords(
            @RequestParam(required = false) String metricType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal principal) {

        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);

        List<HealthCanonicalRecord> records;
        if (metricType != null && !metricType.isBlank()) {
            records = canonicalRecordRepository.findByUserIdAndMetricTypeAndStartTimeBetweenOrderByStartTimeAsc(
                    principal.getUserId(), metricType.toUpperCase(), startDt, endDt);
        } else {
            records = canonicalRecordRepository.findByUserIdAndStartTimeBetweenOrderByStartTimeAsc(
                    principal.getUserId(), startDt, endDt);
        }

        List<HealthCanonicalRecordDto> dtos = records.stream().map(c -> new HealthCanonicalRecordDto(
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

        return ResponseEntity.ok(dtos);
    }
}
