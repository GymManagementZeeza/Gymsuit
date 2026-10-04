package com.zeezaglobal.gymmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthDevice {

    @Id
    @Column(name = "device_id", length = 64)
    private String deviceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 32)
    private String platform; // "IOS", "ANDROID", "WEB", "OTHER"

    @Column(name = "device_type", nullable = false, length = 32)
    private String deviceType; // "PHONE", "WEARABLE", "SCALE", "CGM", "MEDICAL_DEVICE", "MANUAL"

    @Column(length = 64)
    private String manufacturer; // e.g., "Apple", "Samsung", "Garmin", "Google"

    @Column(length = 64)
    private String model; // e.g., "iPhone 15 Pro", "Galaxy Watch 6", "Pixel 9"

    @Column(name = "os_version", length = 32)
    private String osVersion;

    @Column(name = "app_version", length = 32)
    private String appVersion;

    @Column(name = "health_source", length = 32)
    private String healthSource; // "HEALTH_KIT", "HEALTH_CONNECT", "DIRECT_SENSOR", "MANUAL_INPUT"

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
