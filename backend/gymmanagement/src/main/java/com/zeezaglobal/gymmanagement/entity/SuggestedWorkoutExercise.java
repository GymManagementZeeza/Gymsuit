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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One exercise line inside a trainer's suggested workout. Weights are stored in kg. */
@Entity
@Table(name = "suggested_workout_exercises")
@Getter
@Setter
@NoArgsConstructor
public class SuggestedWorkoutExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suggestion_id")
    private TrainerWorkoutSuggestion suggestion;

    @Column(nullable = false)
    private String exerciseName;

    @Column(nullable = false)
    private Integer sets;

    @Column(nullable = false)
    private Integer reps;

    /** Target weight per set in kg; 0 means bodyweight. */
    @Column(nullable = false)
    private Double weightKg;

    @Column(nullable = false)
    private Integer sortOrder;
}
