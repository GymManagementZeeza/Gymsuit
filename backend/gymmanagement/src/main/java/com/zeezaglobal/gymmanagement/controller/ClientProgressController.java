package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.ClientProgressResponse;
import com.zeezaglobal.gymmanagement.service.ClientProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Trainer view of one client's synced progress. Authorization reuses the
 * scoped member check: a trainer may only open their own assigned clients,
 * staff may open any member of the gym.
 */
@RestController
@RequestMapping("/api/gyms/{gymId}/members/{memberId}/progress")
@RequiredArgsConstructor
public class ClientProgressController {

    private final ClientProgressService clientProgressService;

    @GetMapping
    @PreAuthorize("@security.canViewMember(#gymId, #memberId)")
    public ClientProgressResponse getProgress(
            @PathVariable Long gymId,
            @PathVariable Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return clientProgressService.getProgress(gymId, memberId, from, to);
    }
}
