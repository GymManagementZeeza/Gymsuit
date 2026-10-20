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

import java.time.Instant;
import java.time.LocalDate;

/**
 * A step milestone badge earned when a user's reconciled daily step total
 * reaches a threshold (5,000 / 10,000 / 20,000). A badge can only ever be
 * earned once per (user, threshold, date) — re-syncing the same day can
 * never double-award it.
 */
@Entity
@Table(name = "step_badges", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "threshold", "badge_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StepBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Milestone threshold this badge was earned for (e.g. 5000). */
    @Column(nullable = false)
    private int threshold;

    /** Display name, e.g. "Stride Starter". */
    @Column(nullable = false)
    private String badgeName;

    /** The reconciled step total of the day the badge was earned for. */
    @Column(nullable = false)
    private long stepsAchieved;

    /** The walking day (server date as synced), not the award moment. */
    @Column(name = "badge_date", nullable = false)
    private LocalDate badgeDate;

    @Column(nullable = false)
    private Instant awardedAt;
}
