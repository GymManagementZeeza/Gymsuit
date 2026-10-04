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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "health_record_tombstones",
        indexes = {
                @Index(name = "idx_tomb_user_version", columnList = "user_id, server_version")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordTombstone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "canonical_record_id", nullable = false, length = 64)
    private String canonicalRecordId;

    @Column(name = "metric_type", nullable = false, length = 32)
    private String metricType;

    @Column(name = "deleted_at", nullable = false)
    private LocalDateTime deletedAt;

    @Column(name = "server_version", nullable = false)
    private Long serverVersion;

    @Column(name = "deleted_by_device_id", length = 64)
    private String deletedByDeviceId;
}
