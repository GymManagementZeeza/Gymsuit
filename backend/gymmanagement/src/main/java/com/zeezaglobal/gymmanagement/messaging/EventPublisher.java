package com.zeezaglobal.gymmanagement.messaging;

import com.zeezaglobal.gymmanagement.config.RabbitMqConfig;
import com.zeezaglobal.gymmanagement.entity.ActivityType;
import com.zeezaglobal.gymmanagement.entity.NotificationChannel;
import com.zeezaglobal.gymmanagement.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Fire-and-forget publisher for GymSuit's async workloads. Request threads hand work to
 * RabbitMQ and return immediately; consumers do the slow I/O (Resend HTTP calls, DB writes)
 * with retries and dead-lettering.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishEmail(String to, String subject, String htmlBody) {
        EmailEvent event = new EmailEvent(to, subject, htmlBody);
        rabbitTemplate.convertAndSend(RabbitMqConfig.EVENTS_EXCHANGE, RabbitMqConfig.EMAIL_ROUTING_KEY, event);
        log.debug("Published email event for {}", to);
    }

    public void publishNotification(Long gymId, NotificationChannel channel, NotificationType type,
                                    String recipient, String subject, String message) {
        NotificationEvent event = new NotificationEvent(gymId, channel, type, recipient, subject, message);
        rabbitTemplate.convertAndSend(RabbitMqConfig.EVENTS_EXCHANGE, RabbitMqConfig.NOTIFICATION_ROUTING_KEY, event);
        log.debug("Published {} notification event for gym {}", channel, gymId);
    }

    /**
     * Publishes an activity-feed write. When called inside a transaction the event is only
     * published after a successful commit, so rolled-back work never produces activity entries.
     */
    public void publishActivity(Long gymId, ActivityType type, String message) {
        ActivityEvent event = new ActivityEvent(gymId, type, message);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendActivity(event);
                }
            });
        } else {
            sendActivity(event);
        }
    }

    private void sendActivity(ActivityEvent event) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.EVENTS_EXCHANGE, RabbitMqConfig.ACTIVITY_ROUTING_KEY, event);
        log.debug("Published activity event {} for gym {}", event.type(), event.gymId());
    }
}
