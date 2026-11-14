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
 * Meal recommendations a standalone nutritionist sends a linked client for one day. Sending again for the same
 * client and day replaces it. The client reads it in the GymSuit app (Food tab).
 */
@Entity
@Table(name = "meal_plans",
        uniqueConstraints = @UniqueConstraint(columnNames = {"nutritionist_user_id", "member_id", "plan_date"}))
@Getter
@Setter
@NoArgsConstructor
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutritionist_user_id", nullable = false)
    private User nutritionist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(columnDefinition = "TEXT")
    private String breakfast;

    @Column(columnDefinition = "TEXT")
    private String lunch;

    @Column(columnDefinition = "TEXT")
    private String eveningSnack;

    @Column(columnDefinition = "TEXT")
    private String dinner;

    /** Optional note from the nutritionist, e.g. hydration or portion advice. */
    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
