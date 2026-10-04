package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.PointsSyncRequest;
import com.zeezaglobal.gymmanagement.dto.PointsSyncResponse;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.service.PointsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mobile/points")
@RequiredArgsConstructor
@Slf4j
public class PointsController {

    private final PointsSyncService pointsSyncService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<PointsSyncResponse> getPoints(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(pointsSyncService.getPoints(user));
    }

    @PostMapping("/sync")
    public ResponseEntity<PointsSyncResponse> syncPoints(
            @RequestBody PointsSyncRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(pointsSyncService.syncPoints(user, request.deviceId(), request.transactions()));
    }

    @PostMapping("/redeem")
    public ResponseEntity<PointsSyncResponse> redeemPoints(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        int points = body.get("points") instanceof Number ? ((Number) body.get("points")).intValue() : 0;
        String deviceId = body.get("deviceId") != null ? body.get("deviceId").toString() : "unknown-device";
        return ResponseEntity.ok(pointsSyncService.redeem(user, deviceId, points));
    }
}
