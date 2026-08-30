package com.zeezaglobal.gymmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "gyms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Gym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String addressLine;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private String phone;

    @Column(unique = true)
    private String email;

    private String logoUrl;

    /** UPI ID (VPA) used to generate payment QR codes, e.g. for one-time member fees. */
    private String upiId;

    /**
     * One-time fee a new member must pay to finish registration. Nullable so pre-existing gyms
     * (created before this field existed) fall back to "not configured yet" in Settings rather
     * than a false zero — new gyms set this during onboarding, right after setting up plans.
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal joiningFee;

    @Column(length = 3)
    private String joiningFeeCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Manager owner;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "gym_managers",
            joinColumns = @JoinColumn(name = "gym_id"),
            inverseJoinColumns = @JoinColumn(name = "manager_id")
    )
    private Set<Manager> managers = new HashSet<>();
}
