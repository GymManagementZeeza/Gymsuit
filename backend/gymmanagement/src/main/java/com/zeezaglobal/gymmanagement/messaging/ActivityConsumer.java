package com.zeezaglobal.gymmanagement.messaging;

import com.zeezaglobal.gymmanagement.config.RabbitMqConfig;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.service.GymActivityService;
import com.zeezaglobal.gymmanagement.service.GymService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Persists queued gym activity-feed entries. Decouples audit writes from request transactions
 * so a slow database never slows down member check-ins, payments, or subscription changes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityConsumer {

    private final GymService gymService;
    private final GymActivityService activityService;

    @RabbitListener(queues = RabbitMqConfig.ACTIVITY_QUEUE)
    public void handle(ActivityEvent event) {
        Gym gym = gymService.getGymOrThrow(event.gymId());
        activityService.record(gym, event.type(), event.message());
        log.debug("Recorded queued activity {} for gym {}", event.type(), event.gymId());
    }
}
