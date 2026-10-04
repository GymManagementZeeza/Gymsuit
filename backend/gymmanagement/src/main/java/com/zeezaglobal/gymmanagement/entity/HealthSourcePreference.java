package com.zeezaglobal.gymmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "health_source_preferences",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_metric_pref", columnNames = {"user_id", "metric_type"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthSourcePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "metric_type", nullable = false, length = 32)
    private String metricType;

    @Column(name = "preferred_device_types_json", columnDefinition = "TEXT")
    private String preferredDeviceTypesJson;

    @Column(name = "preferred_manufacturers_json", columnDefinition = "TEXT")
    private String preferredManufacturersJson;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
