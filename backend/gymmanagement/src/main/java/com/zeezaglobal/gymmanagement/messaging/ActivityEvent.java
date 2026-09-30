package com.zeezaglobal.gymmanagement.messaging;

import com.zeezaglobal.gymmanagement.entity.ActivityType;

/**
 * Gym activity-feed entry to be persisted asynchronously, decoupled from the request that
 * produced it. Published after the producing transaction commits so rolled-back work never
 * leaves phantom activity entries.
 * Consumed from {@value com.zeezaglobal.gymmanagement.config.RabbitMqConfig#ACTIVITY_QUEUE}.
 */
public record ActivityEvent(
        Long gymId,
        ActivityType type,
        String message
) {
}
