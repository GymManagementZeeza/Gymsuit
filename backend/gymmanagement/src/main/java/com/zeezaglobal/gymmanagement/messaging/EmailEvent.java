package com.zeezaglobal.gymmanagement.messaging;

/**
 * Outbound email to be sent asynchronously (OTP codes, member notifications).
 * Consumed from {@value com.zeezaglobal.gymmanagement.config.RabbitMqConfig#EMAIL_QUEUE}.
 */
public record EmailEvent(
        String to,
        String subject,
        String htmlBody
) {
}
