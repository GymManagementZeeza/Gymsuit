package com.zeezaglobal.gymmanagement.service;

import com.google.firebase.messaging.*;
import com.zeezaglobal.gymmanagement.entity.Challenge;
import com.zeezaglobal.gymmanagement.entity.HealthDevice;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.repository.HealthDeviceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class PushNotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final HealthDeviceRepository deviceRepository;

    @Autowired
    public PushNotificationService(@Autowired(required = false) FirebaseMessaging firebaseMessaging,
                                   HealthDeviceRepository deviceRepository) {
        this.firebaseMessaging = firebaseMessaging;
        this.deviceRepository = deviceRepository;
    }

    /**
     * Send push notification to all active devices of a given user.
     */
    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
        if (userId == null) return;

        List<HealthDevice> devices = deviceRepository.findByUserIdAndActiveTrueAndFcmTokenIsNotNull(userId);
        if (devices.isEmpty()) {
            log.debug("No active registered devices with FCM token for user {}", userId);
            return;
        }

        if (firebaseMessaging == null) {
            log.info("[MOCK PUSH] To User {}: '{}' - '{}' | Data: {}", userId, title, body, data);
            return;
        }

        for (HealthDevice device : devices) {
            String token = device.getFcmToken();
            if (token == null || token.isBlank()) continue;

            try {
                Message.Builder messageBuilder = Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(AndroidNotification.builder()
                                        .setChannelId("gymsuit_challenges")
                                        .setSound("default")
                                        .build())
                                .build())
                        .setApnsConfig(ApnsConfig.builder()
                                .setAps(Aps.builder()
                                        .setSound("default")
                                        .setBadge(1)
                                        .build())
                                .build());

                if (data != null && !data.isEmpty()) {
                    messageBuilder.putAllData(data);
                }

                String response = firebaseMessaging.send(messageBuilder.build());
                log.info("Successfully dispatched FCM message to user {} device {}: {}", userId, device.getDeviceId(), response);
            } catch (FirebaseMessagingException e) {
                log.error("Failed to send FCM push to user {} device {}: {} ({})",
                        userId, device.getDeviceId(), e.getMessage(), e.getMessagingErrorCode());
                // Invalidate stale token if token unregistered
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                        || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                    log.info("Removing invalid FCM token for device {}", device.getDeviceId());
                    device.setFcmToken(null);
                    deviceRepository.save(device);
                }
            } catch (Exception e) {
                log.error("Unexpected error sending FCM notification to device {}: {}", device.getDeviceId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Notify an overtaken participant when another member surpasses them on the challenge leaderboard.
     */
    public void notifyOvertakenParticipant(Challenge challenge,
                                            User overtakingUser,
                                            User overtakenUser,
                                            int previousRank,
                                            int newRank) {
        if (overtakenUser == null || Objects.equals(overtakingUser.getId(), overtakenUser.getId())) {
            return;
        }

        String overtakingName = formatDisplayName(overtakingUser);
        String challengeName = challenge.getName() != null ? challenge.getName() : "Steps Challenge";

        String title = "🏃 Leaderboard Alert: You've been overtaken!";
        String body = String.format("%s just passed you in '%s'! Step up to reclaim your rank #%d!",
                overtakingName, challengeName, previousRank);

        Map<String, String> data = new HashMap<>();
        data.put("type", "CHALLENGE_OVERTAKE");
        data.put("challengeId", String.valueOf(challenge.getId()));
        data.put("challengeName", challengeName);
        data.put("overtakingUserName", overtakingName);
        data.put("previousRank", String.valueOf(previousRank));
        data.put("newRank", String.valueOf(newRank));

        log.info("Triggering overtake push notification: User {} overtaken by User {} in challenge {}",
                overtakenUser.getId(), overtakingUser.getId(), challenge.getId());

        sendToUser(overtakenUser.getId(), title, body, data);
    }

    private String formatDisplayName(User user) {
        if (user == null) return "A challenger";
        Member member = user.getMember();
        if (member != null) {
            String first = member.getFirstName() == null ? "" : member.getFirstName().trim();
            String last = member.getLastName() == null ? "" : member.getLastName().trim();
            String full = (first + " " + last).trim();
            if (!full.isEmpty()) return full;
        }
        String email = user.getEmail();
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "A challenger";
    }
}
