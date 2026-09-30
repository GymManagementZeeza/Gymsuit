package com.zeezaglobal.gymmanagement.messaging;

import com.zeezaglobal.gymmanagement.config.RabbitMqConfig;
import com.zeezaglobal.gymmanagement.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Sends queued outbound emails via Resend. Transient failures are retried with backoff by the
 * listener container; exhausted messages are dead-lettered to {@code gymsuit.email.dlq}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMqConfig.EMAIL_QUEUE)
    public void handle(EmailEvent event) {
        log.info("Dispatching queued email to {}", event.to());
        emailService.sendEmail(event.to(), event.subject(), event.htmlBody());
    }
}
