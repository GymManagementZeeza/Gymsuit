package com.zeezaglobal.gymmanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
        }
    }

    public void sendOtpEmail(String to, String otpCode) {
        String subject = "Your GymSuit Login Code";
        String htmlContent = "<div style=\"font-family: Arial, sans-serif; padding: 20px; color: #333;\">"
                + "<h2>GymSuit Authentication Code</h2>"
                + "<p>Use the following code to complete your login or registration:</p>"
                + "<div style=\"font-size: 32px; font-weight: bold; letter-spacing: 4px; color: #2563eb; margin: 20px 0;\">"
                + otpCode + "</div>"
                + "<p>This code expires in 5 minutes. If you did not request this code, please ignore this email.</p>"
                + "</div>";

        sendEmail(to, subject, htmlContent);
    }

    public void sendNotificationEmail(String to, String title, String message) {
        String subject = "GymSuit Notification: " + title;
        String htmlContent = "<div style=\"font-family: Arial, sans-serif; padding: 20px; color: #333;\">"
                + "<h2>" + title + "</h2>"
                + "<p>" + message + "</p>"
                + "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\" />"
                + "<p style=\"font-size: 12px; color: #777;\">Sent from GymSuit Management Platform</p>"
                + "</div>";

        sendEmail(to, subject, htmlContent);
    }
}
