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
@Table(name = "workout_records", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "external_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    private Integer durationMinutes;

    private Double caloriesBurned;

    private Integer setCount;

    private Integer totalReps;

    private Double totalVolumeKg;

    private String sourceDevice; // e.g. "ANDROID_HEALTH_CONNECT", "IOS_APPLE_HEALTH", "APP"

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
