package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.CheckInResponse;
import com.zeezaglobal.gymmanagement.dto.CheckInUpdateRequest;
import com.zeezaglobal.gymmanagement.service.CheckInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/gyms/{gymId}")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping("/members/{memberId}/checkin")
    @PreAuthorize("@security.canManageCheckIn(#gymId, #memberId)")
    public ResponseEntity<CheckInResponse> checkIn(@PathVariable Long gymId, @PathVariable Long memberId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkInService.checkIn(gymId, memberId));
    }

    @PostMapping("/members/{memberId}/checkout")
    @PreAuthorize("@security.canManageCheckIn(#gymId, #memberId)")
    public CheckInResponse checkOut(@PathVariable Long gymId, @PathVariable Long memberId) {
        return checkInService.checkOut(gymId, memberId);
    }

    @GetMapping("/members/{memberId}/checkins")
    @PreAuthorize("@security.canManageCheckIn(#gymId, #memberId)")
    public List<CheckInResponse> history(@PathVariable Long gymId, @PathVariable Long memberId) {
        return checkInService.history(gymId, memberId);
    }

    @GetMapping("/checkins/active")
    @PreAuthorize("@security.canViewGymActivity(#gymId)")
    public List<CheckInResponse> active(@PathVariable Long gymId) {
        return checkInService.activeInGym(gymId);
    }

    @GetMapping("/checkins")
    @PreAuthorize("@security.canViewGymActivity(#gymId)")
    public List<CheckInResponse> listSince(
            @PathVariable Long gymId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return checkInService.listSince(gymId, since);
    }

    @PutMapping("/checkins/{checkInId}")
    @PreAuthorize("@security.canManageGymCheckIns(#gymId)")
    public CheckInResponse update(
            @PathVariable Long gymId, @PathVariable Long checkInId, @Valid @RequestBody CheckInUpdateRequest request) {
        return checkInService.update(gymId, checkInId, request);
    }

    @PostMapping("/checkins/{checkInId}/force-checkout")
    @PreAuthorize("@security.canManageGymCheckIns(#gymId)")
    public CheckInResponse forceCheckOut(@PathVariable Long gymId, @PathVariable Long checkInId) {
        return checkInService.forceCheckOut(gymId, checkInId);
    }

    @DeleteMapping("/checkins/{checkInId}")
    @PreAuthorize("@security.canManageGymCheckIns(#gymId)")
    public ResponseEntity<Void> delete(@PathVariable Long gymId, @PathVariable Long checkInId) {
        checkInService.delete(gymId, checkInId);
        return ResponseEntity.noContent().build();
    }
}
