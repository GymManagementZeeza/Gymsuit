package com.zeezaglobal.gymmanagement.messaging;

import com.zeezaglobal.gymmanagement.entity.NotificationChannel;
import com.zeezaglobal.gymmanagement.entity.NotificationType;

/**
 * Member notification to be delivered asynchronously. The {@code channel} decides the delivery
 * mechanism (email is sent via Resend today; SMS/WhatsApp providers can be plugged into the
 * consumer later without touching the request path).
 * Consumed from {@value com.zeezaglobal.gymmanagement.config.RabbitMqConfig#NOTIFICATION_QUEUE}.
 */
public record NotificationEvent(
        Long gymId,
        NotificationChannel channel,
        NotificationType type,
        String recipient,
        String subject,
        String message
) {
}
