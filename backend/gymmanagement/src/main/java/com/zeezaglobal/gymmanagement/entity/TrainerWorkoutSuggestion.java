package com.zeezaglobal.gymmanagement.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A workout a trainer sends to one of their clients for a specific day.
 * One row per member per day: sending again the same day replaces the
 * exercise list. The mobile apps read today's row and show it in the
 * workout section; the send also fires a Firebase push and an email.
 */
@Entity
@Table(name = "trainer_workout_suggestions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "suggestion_date"}))
@Getter
@Setter
@NoArgsConstructor
public class TrainerWorkoutSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gym_id")
    private Gym gym;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "suggestion_date", nullable = false)
    private LocalDate suggestionDate;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "suggestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SuggestedWorkoutExercise> exercises = new ArrayList<>();
}
