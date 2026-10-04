package com.zeezaglobal.gymmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "health_raw_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_dedup_hash", columnNames = {"user_id", "dedup_hash"})
        },
        indexes = {
                @Index(name = "idx_raw_user_metric_time", columnList = "user_id, metric_type, start_time, end_time"),
                @Index(name = "idx_raw_user_status", columnList = "user_id, status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthRawRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(name = "metric_type", nullable = false, length = 32)
    private String metricType;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Double value;

    @Column(length = 32)
    private String unit;

    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "source_platform", length = 32)
    private String sourcePlatform;

    @Column(name = "source_device_type", length = 32)
    private String sourceDeviceType;

    @Column(name = "source_manufacturer", length = 64)
    private String sourceManufacturer;

    @Column(name = "source_model", length = 64)
    private String sourceModel;

    @Column(name = "source_system", length = 64)
    private String sourceSystem;

    @Column(name = "source_app_id", length = 128)
    private String sourceAppId;

    @Column(name = "source_record_id", length = 256)
    private String sourceRecordId;

    @Column(name = "dedup_hash", nullable = false, length = 64)
    private String dedupHash;

    private Double confidence;

    @Column(nullable = false, length = 32)
    private String status = "RAW"; // "RAW", "RECONCILED", "ARCHIVED"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "client_sync_at")
    private LocalDateTime clientSyncAt;
}
