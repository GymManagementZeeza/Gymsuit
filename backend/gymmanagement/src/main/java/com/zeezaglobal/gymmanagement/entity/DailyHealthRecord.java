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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_health_records", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "record_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyHealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    private Long steps;

    private Double activeCalories;

    private Double totalCalories;

    private Double distanceMeters;

    private Integer latestHeartRateBpm;

    private Integer restingHeartRateBpm;

    private Integer minHeartRateBpm;

    private Integer maxHeartRateBpm;

    @Column(columnDefinition = "TEXT")
    private String heartRateSamplesJson;

    private Long sleepDurationMinutes;

    private LocalDateTime sleepStartTime;

    private LocalDateTime sleepEndTime;

    @Column(columnDefinition = "TEXT")
    private String sleepStagesJson;

    private Double weightKg;

    private String sourceDevice; // e.g. "ANDROID_HEALTH_CONNECT", "IOS_APPLE_HEALTH"

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
