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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "members", uniqueConstraints = @UniqueConstraint(columnNames = {"gym_id", "email"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String email;

    @Column(nullable = false)
    private String phone;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Double heightCm;

    private Double weightKg;

    private Double goalWeightKg;

    private LocalDate goalTargetDate;

    private String bloodGroup;

    @Column(columnDefinition = "TEXT")
    private String medicalNotes;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private String emergencyContactRelationship;

    @Column(nullable = false)
    private boolean waiverAccepted;

    @Column(nullable = false)
    private LocalDate joinDate;

    /** Whether this member has paid their gym's one-time joining fee — required to finish registration. */
    @Column(nullable = false)
    private boolean joiningFeePaid;

    private LocalDateTime joiningFeePaidAt;
}
