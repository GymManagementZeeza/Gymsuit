package com.zeezaglobal.gymmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.Instant;

/**
 * A standalone nutritionist's email invitation to a client. The token in the emailed link is the only
 * credential needed to accept; accepting links the GymSuit member account with that email to the nutritionist.
 */
@Entity
@Table(name = "nutritionist_invites")
@Getter
@Setter
@NoArgsConstructor
public class NutritionistInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutritionist_user_id", nullable = false)
    private User nutritionist;

    @Column(nullable = false)
    private String email;

    private String firstName;

    private String lastName;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainerInviteStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant acceptedAt;
}
