package com.zeezaglobal.gymmanagement.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${app.firebase.credentials-path:}")
    private String credentialsPath;

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        InputStream serviceAccountStream = null;
        try {
            if (credentialsPath != null && !credentialsPath.isBlank()) {
                Resource resource = new FileSystemResource(credentialsPath);
                if (resource.exists()) {
                    serviceAccountStream = resource.getInputStream();
                    log.info("Loading Firebase credentials from file: {}", credentialsPath);
                } else {
                    resource = new ClassPathResource(credentialsPath);
                    if (resource.exists()) {
                        serviceAccountStream = resource.getInputStream();
                        log.info("Loading Firebase credentials from classpath: {}", credentialsPath);
                    }
                }
            }

            // Fallback: check classpath for standard firebase-service-account.json
            if (serviceAccountStream == null) {
                Resource defaultResource = new ClassPathResource("firebase-service-account.json");
                if (defaultResource.exists()) {
                    serviceAccountStream = defaultResource.getInputStream();
                    log.info("Loading Firebase credentials from default classpath: firebase-service-account.json");
                }
            }

            if (serviceAccountStream != null) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                        .build();
                FirebaseApp app = FirebaseApp.initializeApp(options);
                log.info("FirebaseApp initialized successfully: {}", app.getName());
                return app;
            } else {
                log.warn("No Firebase credentials found. Push notifications will run in mock mode.");
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to initialize FirebaseApp: {}. Push notifications will run in mock mode.", e.getMessage(), e);
            return null;
        } finally {
            if (serviceAccountStream != null) {
                try {
                    serviceAccountStream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        if (firebaseApp == null) {
            log.warn("FirebaseApp is not configured; FirebaseMessaging bean is disabled.");
            return null;
        }
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
