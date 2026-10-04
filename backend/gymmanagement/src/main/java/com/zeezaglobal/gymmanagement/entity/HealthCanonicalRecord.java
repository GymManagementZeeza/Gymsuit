package com.zeezaglobal.gymmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "health_canonical_records",
        indexes = {
                @Index(name = "idx_can_user_version", columnList = "user_id, server_version"),
                @Index(name = "idx_can_user_metric_time", columnList = "user_id, metric_type, start_time, end_time")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthCanonicalRecord {

    @Id
    @Column(length = 64)
    private String id; // UUID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    @Column(name = "selected_raw_record_id")
    private Long selectedRawRecordId;

    @Column(name = "selected_device_id", length = 64)
    private String selectedDeviceId;

    @Column(name = "selected_source_system", length = 64)
    private String selectedSourceSystem;

    private Double confidence;

    @Column(name = "server_version", nullable = false)
    private Long serverVersion;

    @Column(name = "reconciled_at", nullable = false)
    private LocalDateTime reconciledAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}
