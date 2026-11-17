package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.WorkoutAiLogRequest;
import com.zeezaglobal.gymmanagement.dto.WorkoutResponse;
import com.zeezaglobal.gymmanagement.dto.WorkoutSyncRequest;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.service.WorkoutAiService;
import com.zeezaglobal.gymmanagement.service.WorkoutIntakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Mobile / MCP workout log. Mirrors {@link MobileFoodController}: an app or an
 * MCP client uploads workout sessions (strength or cardio) by the caller's
 * entry id, lists a recent window to merge, and deletes by entry id. Calorie
 * burn is estimated server-side when the caller doesn't provide it.
 */
@RestController
@RequestMapping("/api/mobile/workout")
@RequiredArgsConstructor
public class MobileWorkoutController {

    private final WorkoutIntakeService workoutIntakeService;
    private final WorkoutAiService workoutAiService;
    private final SecurityService securityService;

    private UserPrincipal requireUser() {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal;
    }

    /** Upsert workout sessions; returns the saved records (with estimated calories). */
    @PostMapping("/sync")
    public List<WorkoutResponse> sync(@Valid @RequestBody WorkoutSyncRequest request) {
        return workoutIntakeService.sync(request, requireUser());
    }

    /**
     * Describe the day's training in plain language; the AI turns it into
     * structured workouts which are saved (calories estimated) and returned.
     */
    @PostMapping("/ai-log")
    public List<WorkoutResponse> aiLog(@Valid @RequestBody WorkoutAiLogRequest request) {
        UserPrincipal user = requireUser();
        LocalDate date = request.date() != null ? request.date() : LocalDate.now();
        var entries = workoutAiService.parse(request.text(), date);
        return workoutIntakeService.sync(new WorkoutSyncRequest(entries), user);
    }

    /**
     * The signed-in member's workouts from {@code from} (YYYY-MM-DD, default 30
     * days ago) through today, so the app can merge workouts logged elsewhere.
     */
    @GetMapping
    public List<WorkoutResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
        return workoutIntakeService.listForUser(requireUser(), from);
    }

    @DeleteMapping("/{externalId}")
    public void delete(@PathVariable String externalId) {
        workoutIntakeService.deleteByExternalId(externalId, requireUser());
    }
}
