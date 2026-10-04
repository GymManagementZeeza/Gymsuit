package com.zeezaglobal.gymmanagement.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HealthReconciliationConfig {

    private final Map<String, List<String>> defaultTypePriorities = new HashMap<>();

    public HealthReconciliationConfig() {
        defaultTypePriorities.put("STEPS", List.of("WEARABLE", "PHONE", "MANUAL"));
        defaultTypePriorities.put("HEART_RATE", List.of("WEARABLE", "MEDICAL_DEVICE", "PHONE", "MANUAL"));
        defaultTypePriorities.put("RESTING_HEART_RATE", List.of("WEARABLE", "MEDICAL_DEVICE", "PHONE", "MANUAL"));
        defaultTypePriorities.put("SLEEP", List.of("WEARABLE", "PHONE", "MANUAL"));
        defaultTypePriorities.put("CALORIES_ACTIVE", List.of("WEARABLE", "PHONE", "MANUAL"));
        defaultTypePriorities.put("CALORIES_TOTAL", List.of("WEARABLE", "PHONE", "MANUAL"));
        defaultTypePriorities.put("DISTANCE", List.of("WEARABLE", "PHONE", "MANUAL"));
        defaultTypePriorities.put("BLOOD_OXYGEN", List.of("MEDICAL_DEVICE", "WEARABLE", "PHONE", "MANUAL"));
        defaultTypePriorities.put("WEIGHT", List.of("SCALE", "MEDICAL_DEVICE", "MANUAL", "WEARABLE", "PHONE"));
        defaultTypePriorities.put("EXERCISE", List.of("WEARABLE", "PHONE", "MANUAL"));
    }

    public List<String> getTypePriority(String metricType) {
        if (metricType == null) return List.of("WEARABLE", "PHONE", "MANUAL");
        return defaultTypePriorities.getOrDefault(metricType.toUpperCase(), List.of("WEARABLE", "PHONE", "MANUAL"));
    }

    public double calculateSourceScore(String metricType, String deviceType, String manufacturer, Double confidence) {
        List<String> priorities = getTypePriority(metricType);
        double typeScore = 100.0;
        if (deviceType != null) {
            String norm = deviceType.toUpperCase();
            int idx = priorities.indexOf(norm);
            if (idx >= 0) {
                typeScore = (priorities.size() - idx) * 500.0;
            } else if (norm.contains("WATCH") || norm.contains("WEARABLE") || norm.contains("BAND")) {
                typeScore = 1500.0;
            }
        }

        double manufacturerScore = 0.0;
        if (manufacturer != null) {
            String m = manufacturer.toUpperCase();
            if (m.contains("APPLE") || m.contains("SAMSUNG") || m.contains("GARMIN") || m.contains("WHOOP") || m.contains("WITHINGS") || m.contains("FITBIT")) {
                manufacturerScore = 100.0;
            }
        }

        double conf = (confidence != null && confidence > 0) ? confidence : 0.8;
        return (typeScore + manufacturerScore) * conf;
    }
}
