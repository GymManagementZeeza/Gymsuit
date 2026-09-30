package com.zeezaglobal.gymmanagement.messaging;

import com.zeezaglobal.gymmanagement.config.RabbitMqConfig;
import com.zeezaglobal.gymmanagement.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Delivers queued member notifications. EMAIL goes out via Resend today; SMS and WHATSAPP have
 * no provider wired up yet, so those are logged and skipped — plug a provider in here later
 * without touching the request path.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_QUEUE)
    public void handle(NotificationEvent event) {
        switch (event.channel()) {
            case EMAIL -> {
                log.info("Dispatching queued email notification to {}", event.recipient());
                emailService.sendNotificationEmail(event.recipient(), event.subject(), event.message());
            }
            case SMS, WHATSAPP -> log.warn(
                    "No {} provider configured; notification for gym {} to {} was not delivered: {}",
                    event.channel(), event.gymId(), event.recipient(), event.message());
        }
    }
}
