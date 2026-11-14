package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.MealPlanRequest;
import com.zeezaglobal.gymmanagement.dto.MealPlanResponse;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.service.MealPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Meal plans. Nutritionists send and list them for a linked client (role and link checked in the service);
 * the signed-in app member reads their current plan.
 */
@RestController
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final SecurityService securityService;

    @PostMapping("/api/nutritionist/clients/{memberId}/meal-plans")
    public ResponseEntity<MealPlanResponse> send(@PathVariable Long memberId, @Valid @RequestBody MealPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mealPlanService.send(memberId, request));
    }

    @GetMapping("/api/nutritionist/clients/{memberId}/meal-plans")
    public List<MealPlanResponse> history(@PathVariable Long memberId) {
        return mealPlanService.history(memberId);
    }

    /** Mobile apps: today's meal plan for the signed-in member, or the latest earlier one. 404 when none. */
    @GetMapping("/api/mobile/meal-plan/current")
    public ResponseEntity<MealPlanResponse> current() {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (principal.getMemberId() == null) {
            return ResponseEntity.notFound().build();
        }
        return mealPlanService.currentForMember(principal.getMemberId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
