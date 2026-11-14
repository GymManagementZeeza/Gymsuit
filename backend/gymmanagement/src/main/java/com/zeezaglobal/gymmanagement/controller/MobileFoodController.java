package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.EatingHabitScoreResponse;
import com.zeezaglobal.gymmanagement.dto.FoodIntakeSyncRequest;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.service.EatingHabitScoreService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/mobile/food")
@RequiredArgsConstructor
public class MobileFoodController {

    private final FoodIntakeService foodIntakeService;
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

    @PostMapping("/sync")
    public Map<String, Integer> sync(@Valid @RequestBody FoodIntakeSyncRequest request) {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return Map.of("synced", foodIntakeService.sync(request, principal));
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
