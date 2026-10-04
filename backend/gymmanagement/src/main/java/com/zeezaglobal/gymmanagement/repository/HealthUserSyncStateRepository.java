package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.HealthUserSyncState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthUserSyncStateRepository extends JpaRepository<HealthUserSyncState, Long> {
}
