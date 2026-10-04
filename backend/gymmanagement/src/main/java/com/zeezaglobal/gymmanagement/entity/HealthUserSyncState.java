package com.zeezaglobal.gymmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "health_user_sync_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthUserSyncState {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "last_version", nullable = false)
    private Long lastVersion = 0L;

    @Column(name = "last_reconciled_at")
    private LocalDateTime lastReconciledAt;
}
