package com.zeezaglobal.gymmanagement.exception;

/**
 * Thrown when an outbound email cannot be delivered (e.g. the Resend API call fails).
 * In the RabbitMQ consumers this triggers the retry policy and, after retries are exhausted,
 * dead-letters the message for later inspection.
 */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
