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

/**
 * A member who accepted a standalone nutritionist's invite. This link is the only thing that lets a
 * nutritionist (who belongs to no gym) read that member's food log, macros and weight.
 */
@Entity
@Table(name = "nutritionist_clients",
        uniqueConstraints = @UniqueConstraint(columnNames = {"nutritionist_user_id", "member_id"}))
@Getter
@Setter
@NoArgsConstructor
public class NutritionistClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The NUTRITIONIST user account. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutritionist_user_id", nullable = false)
    private User nutritionist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Instant linkedAt;
}
