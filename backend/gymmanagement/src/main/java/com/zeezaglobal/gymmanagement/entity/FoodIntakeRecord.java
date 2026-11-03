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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One food-log entry uploaded from the member's mobile app (the app's
 * food_log.json entry). Synced idempotently by the app's entry id, so
 * re-uploading the same log never double-counts calories. This is the only
 * calorie-intake source the trainer progress page reads — intake that stays
 * on the device alone never appears here.
 */
@Entity
@Table(name = "food_intake_records", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "external_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class FoodIntakeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String externalId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    /** BREAKFAST / LUNCH / DINNER / SNACK, as stored by the app. */
    private String meal;

    private String dishName;

    private Integer calories;

    private Double proteinG;

    private Double carbsG;

    private Double fatG;

    /**
     * The meal photo the client took when logging this entry, compressed
     * (client-side JPEG, longest side 1024px) before upload. Stored as
     * bytes so the trainer progress page can serve it without a separate
     * object store; null for manually typed entries with no photo.
     */
    @jakarta.persistence.Lob
    @Column(name = "photo_data")
    private byte[] photoData;

    private String photoContentType;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
