package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.exception.EmailDeliveryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Value("${app.resend.api-key:}")
    private String apiKey;

    @Value("${app.resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    private final RestClient restClient;

    public EmailService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .build();
    }

    /**
     * Sends an email via Resend. Throws {@link EmailDeliveryException} when the API call fails so
     * RabbitMQ consumers can retry / dead-letter the message. A missing API key is a configuration
     * problem that retries cannot fix, so it only logs and skips.
     */
    public void sendEmail(String to, String subject, String htmlContent) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("RESEND_API_KEY is not set. Skipping email dispatch to {}. Content subject: {}", to, subject);
            return;
        }

        try {
            Map<String, Object> body = Map.of(
                    "from", fromEmail,
                    "to", List.of(to),
                    "subject", subject,
                    "html", htmlContent
            );

            restClient.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully sent email via Resend to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {} via Resend API: {}", to, e.getMessage(), e);
            throw new EmailDeliveryException("Failed to send email to " + to, e);
        }
    }

    /**
     * Sends an email with a single file attachment via Resend.
     * Same failure semantics as {@link #sendEmail}: throws {@link EmailDeliveryException}
     * on API failure, skips quietly when no API key is configured.
     */
    public void sendEmailWithAttachment(String to, String subject, String htmlContent,
                                        String filename, byte[] fileBytes, String contentType) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("RESEND_API_KEY is not set. Skipping email dispatch to {}. Content subject: {}", to, subject);
            return;
        }

        try {
            Map<String, Object> attachment = Map.of(
                    "content", Base64.getEncoder().encodeToString(fileBytes),
                    "filename", filename,
                    "content_type", contentType
            );
            Map<String, Object> body = Map.of(
                    "from", fromEmail,
                    "to", List.of(to),
                    "subject", subject,
                    "html", htmlContent,
                    "attachments", List.of(attachment)
            );

            restClient.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully sent email with attachment {} via Resend to {}", filename, to);
        } catch (Exception e) {
            log.error("Failed to send email with attachment to {} via Resend API: {}", to, e.getMessage(), e);
            throw new EmailDeliveryException("Failed to send email to " + to, e);
        }
    }

    /**
     * Sends an email with one file attachment plus inline images via Resend.
     * Each inline image is sent with a {@code content_id} so the HTML can
     * reference it as {@code <img src="cid:<content_id>">} — this is how
     * Resend embeds images that render inside the message body (e.g. the
     * health report's step progress rings). Same failure semantics as
     * {@link #sendEmailWithAttachment}: throws {@link EmailDeliveryException}
     * on API failure, skips quietly when no API key is configured.
     */
    public void sendEmailWithInlineImagesAndAttachment(String to, String subject, String htmlContent,
                                                       Map<String, byte[]> inlineImages,
                                                       String filename, byte[] fileBytes, String contentType) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("RESEND_API_KEY is not set. Skipping email dispatch to {}. Content subject: {}", to, subject);
            return;
        }

        try {
            List<Object> attachments = new ArrayList<>();
            for (Map.Entry<String, byte[]> image : inlineImages.entrySet()) {
                attachments.add(Map.of(
                        "content", Base64.getEncoder().encodeToString(image.getValue()),
                        "filename", image.getKey() + ".png",
                        "content_type", "image/png",
                        "content_id", image.getKey()
                ));
            }
            attachments.add(Map.of(
                    "content", Base64.getEncoder().encodeToString(fileBytes),
                    "filename", filename,
                    "content_type", contentType
            ));
            Map<String, Object> body = Map.of(
                    "from", fromEmail,
                    "to", List.of(to),
                    "subject", subject,
                    "html", htmlContent,
                    "attachments", attachments
            );

            restClient.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully sent email with attachment {} and {} inline images via Resend to {}",
                    filename, inlineImages.size(), to);
        } catch (Exception e) {
            log.error("Failed to send email with attachment to {} via Resend API: {}", to, e.getMessage(), e);
            throw new EmailDeliveryException("Failed to send email to " + to, e);
        }
    }

    /**
     * A single image embedded inline in the email body (referenced in the
     * HTML as {@code <img src="cid:<contentId>">}).
     */
    public record InlineImage(String contentId, String filename, String contentType, byte[] content) {}

    /**
     * Sends an email with inline images and no file attachment.
     * Same failure semantics as {@link #sendEmail}: throws
     * {@link EmailDeliveryException} on API failure, skips quietly when no
     * API key is configured.
     */
    public void sendEmailWithInlineImages(String to, String subject, String htmlContent,
                                          List<InlineImage> inlineImages) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("RESEND_API_KEY is not set. Skipping email dispatch to {}. Content subject: {}", to, subject);
            return;
        }

        try {
            List<Object> attachments = new ArrayList<>();
            for (InlineImage image : inlineImages) {
                attachments.add(Map.of(
                        "content", Base64.getEncoder().encodeToString(image.content()),
                        "filename", image.filename(),
                        "content_type", image.contentType(),
                        "content_id", image.contentId()
                ));
            }
            Map<String, Object> body = Map.of(
                    "from", fromEmail,
                    "to", List.of(to),
                    "subject", subject,
                    "html", htmlContent,
                    "attachments", attachments
            );

            restClient.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully sent email with {} inline image(s) via Resend to {}", inlineImages.size(), to);
        } catch (Exception e) {
            log.error("Failed to send email with inline images to {} via Resend API: {}", to, e.getMessage(), e);
            throw new EmailDeliveryException("Failed to send email to " + to, e);
        }
    }

    public static String otpEmailHtml(String otpCode) {
        return "<div style=\"font-family: Arial, sans-serif; padding: 20px; color: #333;\">"
                + "<h2>GymSuit Authentication Code</h2>"
                + "<p>Use the following code to complete your login or registration:</p>"
                + "<div style=\"font-size: 32px; font-weight: bold; letter-spacing: 4px; color: #2563eb; margin: 20px 0;\">"
                + otpCode + "</div>"
                + "<p>This code expires in 5 minutes. If you did not request this code, please ignore this email.</p>"
                + "</div>";
    }

    public static String notificationEmailHtml(String title, String message) {
        return "<div style=\"font-family: Arial, sans-serif; padding: 20px; color: #333;\">"
                + "<h2>" + title + "</h2>"
                + "<p>" + message + "</p>"
                + "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\" />"
                + "<p style=\"font-size: 12px; color: #777;\">Sent from GymSuit Management Platform</p>"
                + "</div>";
    }

    public void sendOtpEmail(String to, String otpCode) {
        sendEmail(to, "Your GymSuit Login Code", otpEmailHtml(otpCode));
    }

    public void sendNotificationEmail(String to, String title, String message) {
        sendEmail(to, "GymSuit Notification: " + title, notificationEmailHtml(title, message));
    }
}
