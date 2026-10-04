package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.HealthRecordTombstone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthRecordTombstoneRepository extends JpaRepository<HealthRecordTombstone, Long> {
    List<HealthRecordTombstone> findByUserIdAndServerVersionGreaterThanOrderByServerVersionAsc(
            Long userId, Long serverVersion);

    boolean existsByUserIdAndCanonicalRecordId(Long userId, String canonicalRecordId);
}
