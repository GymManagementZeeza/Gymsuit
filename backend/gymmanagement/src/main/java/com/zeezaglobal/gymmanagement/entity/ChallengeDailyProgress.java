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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One participant's value for a single calendar day of a challenge.
 * Clients report per-day values from on-device health data (Health Connect /
 * HealthKit); the server stores them so every participant can see each other's
 * daily breakdown and the app can draw progress graphs.
 */
@Entity
@Table(name = "challenge_daily_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"challenge_id", "user_id", "progress_date"}))
@Getter
@Setter
@NoArgsConstructor
public class ChallengeDailyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "progress_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private double value;

    @Column(nullable = false)
    private Instant updatedAt;
}
