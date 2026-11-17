package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.EatingHabitScoreResponse;
import com.zeezaglobal.gymmanagement.dto.FoodIntakeResponse;
import com.zeezaglobal.gymmanagement.dto.FoodAiLogRequest;
import com.zeezaglobal.gymmanagement.dto.FoodIntakeSyncRequest;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.service.EatingHabitScoreService;
import com.zeezaglobal.gymmanagement.service.FoodAiService;
import com.zeezaglobal.gymmanagement.service.FoodIntakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mobile/food")
@RequiredArgsConstructor
public class MobileFoodController {

    private final FoodIntakeService foodIntakeService;
    private final FoodAiService foodAiService;
    private final EatingHabitScoreService eatingHabitScoreService;
    private final SecurityService securityService;

    /**
     * Overall eating-habit score (0–100) from the food the signed-in
     * member logged over the trailing {@code days} (default 7, max 30).
     * {@code calorieTargetKcal} defaults to the 2000 kcal the app shows.
     * The score is null with label "No data yet" when nothing was logged.
     */
    @GetMapping("/score")
    public EatingHabitScoreResponse score(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "2000") int calorieTargetKcal) {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return eatingHabitScoreService.scoreForUser(principal.getUserId(), days, calorieTargetKcal);
    }

    /**
     * All of the signed-in member's food entries from {@code from} (YYYY-MM-DD,
     * default 30 days ago) through today, so the app can merge meals logged on
     * another device, the diet website or an MCP client into its on-device log.
     */
    @GetMapping
    public List<FoodIntakeResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return foodIntakeService.listForUser(principal, from);
    }

    @PostMapping("/sync")
    public Map<String, Integer> sync(@Valid @RequestBody FoodIntakeSyncRequest request) {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return Map.of("synced", foodIntakeService.sync(request, principal));
    }

    /**
     * Describe what was eaten in plain language; the AI turns it into meals with
     * estimated nutrition, which are saved and returned for the app to merge.
     */
    @PostMapping("/ai-log")
    public List<FoodIntakeResponse> aiLog(@Valid @RequestBody FoodAiLogRequest request) {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        LocalDate date = request.date() != null ? request.date() : LocalDate.now();
        var entries = foodAiService.parse(request.text(), date);
        foodIntakeService.sync(new FoodIntakeSyncRequest(entries), principal);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return entries.stream()
                .map(e -> new FoodIntakeResponse(e.externalId(), e.recordDate(), e.meal(), e.dishName(),
                        e.calories(), e.proteinG(), e.carbsG(), e.fatG(), false, now))
                .toList();
    }

    @DeleteMapping("/{externalId}")
    public void delete(@PathVariable String externalId) {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        foodIntakeService.deleteByExternalId(externalId, principal);
    }
}
