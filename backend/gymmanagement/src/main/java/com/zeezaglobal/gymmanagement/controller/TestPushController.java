package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Diagnostic endpoint: sends a test push notification to the caller's own
 * devices and reports whether Firebase is configured on this server and how
 * many of the caller's devices have a stored FCM token. A test that reports
 * sent=true but never arrives on the phone points at the phone (notification
 * permission / OS battery handling); sent=false names the backend cause.
 */
@RestController
@RequestMapping("/api/admin/test-push")
@RequiredArgsConstructor
public class TestPushController {

    private final PushNotificationService pushNotificationService;
    private final SecurityService securityService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Map<String, Object>> sendTestPush() {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        int devices = pushNotificationService.countDevicesWithTokens(principal.getUserId());
        boolean configured = pushNotificationService.isFirebaseConfigured();
        if (configured && devices > 0) {
            pushNotificationService.sendToUser(
                    principal.getUserId(),
                    "GymSuit test notification",
                    "If you can read this, Firebase push is working.",
                    Map.of("type", "TEST_PUSH"),
                    "gymsuit_challenges");
        }
        return ResponseEntity.ok(Map.of(
                "firebaseConfigured", configured,
                "devicesTargeted", devices,
                "sent", configured && devices > 0));
    }
}
