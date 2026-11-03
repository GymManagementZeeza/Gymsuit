package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.FoodIntakeSyncRequest;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.service.FoodIntakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mobile/food")
@RequiredArgsConstructor
public class MobileFoodController {

    private final FoodIntakeService foodIntakeService;
    private final SecurityService securityService;

    @PostMapping("/sync")
    public Map<String, Integer> sync(@Valid @RequestBody FoodIntakeSyncRequest request) {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return Map.of("synced", foodIntakeService.sync(request, principal));
    }
}
