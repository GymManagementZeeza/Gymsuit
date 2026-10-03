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

@Entity
@Table(name = "challenge_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"challenge_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ChallengeParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant joinedAt;

    @Column(nullable = false)
    private long stepsTotal = 0;

    @Column(nullable = false)
    private int workoutsTotal = 0;

    @Column(nullable = false)
    private double caloriesTotal = 0;

    @Column(nullable = false)
    private double distanceKmTotal = 0;

    private Instant lastProgressAt;
}
