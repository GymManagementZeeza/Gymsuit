package com.zeezaglobal.gymmanagement.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for GymSuit's async workloads.
 *
 * <p>One topic exchange ({@value #EVENTS_EXCHANGE}) routes domain events to durable queues:
 * <ul>
 *   <li>{@value #EMAIL_QUEUE} — outbound emails (OTP codes, member notifications) sent via Resend</li>
 *   <li>{@value #NOTIFICATION_QUEUE} — member notifications fan-out (email now, SMS/WhatsApp later)</li>
 *   <li>{@value #ACTIVITY_QUEUE} — gym activity-feed writes, decoupled from request transactions</li>
 * </ul>
 * Each queue dead-letters to {@value #DLX_EXCHANGE} so poison messages land in a DLQ instead of
 * being retried forever, and the listener container retries transient failures with backoff first.
 */
@Configuration
public class RabbitMqConfig {

    public static final String EVENTS_EXCHANGE = "gymsuit.events";
    public static final String DLX_EXCHANGE = "gymsuit.dlx";

    public static final String EMAIL_QUEUE = "gymsuit.email";
    public static final String NOTIFICATION_QUEUE = "gymsuit.notification";
    public static final String ACTIVITY_QUEUE = "gymsuit.activity";

    public static final String EMAIL_ROUTING_KEY = "email.send";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.send";
    public static final String ACTIVITY_ROUTING_KEY = "activity.record";

    @Value("${app.rabbitmq.listener.concurrency:2}")
    private int listenerConcurrency;

    @Value("${app.rabbitmq.listener.max-concurrency:8}")
    private int listenerMaxConcurrency;

    @Value("${app.rabbitmq.listener.prefetch:20}")
    private int listenerPrefetch;

    @Value("${app.rabbitmq.listener.retry.max-attempts:3}")
    private int retryMaxAttempts;

    @Value("${app.rabbitmq.listener.retry.initial-interval-ms:2000}")
    private long retryInitialIntervalMs;

    @Value("${app.rabbitmq.listener.retry.max-interval-ms:15000}")
    private long retryMaxIntervalMs;

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    private Queue durableQueueWithDlq(String name, String dlqRoutingKey) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    private Queue deadLetterQueue(String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    public Queue emailQueue() {
        return durableQueueWithDlq(EMAIL_QUEUE, EMAIL_QUEUE + ".dlq");
    }

    @Bean
    public Queue notificationQueue() {
        return durableQueueWithDlq(NOTIFICATION_QUEUE, NOTIFICATION_QUEUE + ".dlq");
    }

    @Bean
    public Queue activityQueue() {
        return durableQueueWithDlq(ACTIVITY_QUEUE, ACTIVITY_QUEUE + ".dlq");
    }

    @Bean
    public Queue emailDeadLetterQueue() {
        return deadLetterQueue(EMAIL_QUEUE + ".dlq");
    }

    @Bean
    public Queue notificationDeadLetterQueue() {
        return deadLetterQueue(NOTIFICATION_QUEUE + ".dlq");
    }

    @Bean
    public Queue activityDeadLetterQueue() {
        return deadLetterQueue(ACTIVITY_QUEUE + ".dlq");
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(emailQueue).to(eventsExchange).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(eventsExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding activityBinding(Queue activityQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(activityQueue).to(eventsExchange).with(ACTIVITY_ROUTING_KEY);
    }

    @Bean
    public Binding emailDlqBinding(Queue emailDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(emailDeadLetterQueue).to(deadLetterExchange).with(EMAIL_QUEUE + ".dlq");
    }

    @Bean
    public Binding notificationDlqBinding(Queue notificationDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(notificationDeadLetterQueue).to(deadLetterExchange).with(NOTIFICATION_QUEUE + ".dlq");
    }

    @Bean
    public Binding activityDlqBinding(Queue activityDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(activityDeadLetterQueue).to(deadLetterExchange).with(ACTIVITY_QUEUE + ".dlq");
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter);
        return template;
    }

    /**
     * Listener factory shared by all consumers: JSON conversion, horizontal consumer scaling,
     * bounded prefetch, and bounded retries with exponential backoff. Messages that still fail
     * are rejected without requeue so the queue's DLX routes them to the DLQ.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        factory.setConcurrentConsumers(listenerConcurrency);
        factory.setMaxConcurrentConsumers(listenerMaxConcurrency);
        factory.setPrefetchCount(listenerPrefetch);
        factory.setDefaultRequeueRejected(false);
        factory.setMissingQueuesFatal(false);
        factory.setAdviceChain(
                RetryInterceptorBuilder.stateless()
                        .maxRetries(retryMaxAttempts)
                        .backOffOptions(retryInitialIntervalMs, 2.0, retryMaxIntervalMs)
                        .build());

        return factory;
    }
}
