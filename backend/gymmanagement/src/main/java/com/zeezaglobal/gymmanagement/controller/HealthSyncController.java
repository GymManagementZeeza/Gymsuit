package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.DailyHealthSyncDto;
import com.zeezaglobal.gymmanagement.dto.HealthSyncRequest;
import com.zeezaglobal.gymmanagement.dto.HealthSyncResponse;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.service.HealthSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/mobile/health")
@RequiredArgsConstructor
public class HealthSyncController {

    private final HealthSyncService healthSyncService;
    private final SecurityService securityService;

    private UserPrincipal currentUser() {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal;
    }

    @PostMapping("/sync")
    public ResponseEntity<HealthSyncResponse> sync(@RequestBody HealthSyncRequest request) {
        HealthSyncResponse response = healthSyncService.sync(request, currentUser());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/records")
    public ResponseEntity<List<DailyHealthSyncDto>> getRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(healthSyncService.getRecords(startDate, endDate, currentUser()));
    }
}
