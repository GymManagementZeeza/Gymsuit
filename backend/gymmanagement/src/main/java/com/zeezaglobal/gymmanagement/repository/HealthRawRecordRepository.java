package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.HealthRawRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthRawRecordRepository extends JpaRepository<HealthRawRecord, Long> {
    boolean existsByUserIdAndDedupHash(Long userId, String dedupHash);
    Optional<HealthRawRecord> findByUserIdAndDedupHash(Long userId, String dedupHash);

    List<HealthRawRecord> findByUserIdAndMetricTypeAndStartTimeBetweenOrderByStartTimeAsc(
            Long userId, String metricType, LocalDateTime start, LocalDateTime end);

    List<HealthRawRecord> findByUserIdAndStartTimeBetweenOrderByStartTimeAsc(
            Long userId, LocalDateTime start, LocalDateTime end);
}
