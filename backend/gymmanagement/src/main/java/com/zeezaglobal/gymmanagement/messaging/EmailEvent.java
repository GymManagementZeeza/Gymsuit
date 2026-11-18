package com.zeezaglobal.gymmanagement.messaging;

/**
 * Outbound email to be sent asynchronously (OTP codes, member notifications).
 * Consumed from {@value com.zeezaglobal.gymmanagement.config.RabbitMqConfig#EMAIL_QUEUE}.
 * {@code textBody} is an optional plain-text part (null = HTML only); messages queued before it
 * existed deserialize with null.
 */
public record EmailEvent(
        String to,
        String subject,
        String htmlBody,
        String textBody
) {
}
