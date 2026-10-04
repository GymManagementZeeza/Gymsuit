package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.HealthCanonicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthCanonicalRecordRepository extends JpaRepository<HealthCanonicalRecord, String> {

    List<HealthCanonicalRecord> findByUserIdAndServerVersionGreaterThanOrderByServerVersionAsc(
            Long userId, Long serverVersion);

    List<HealthCanonicalRecord> findByUserIdAndMetricTypeAndStartTimeBetweenOrderByStartTimeAsc(
            Long userId, String metricType, LocalDateTime start, LocalDateTime end);

    List<HealthCanonicalRecord> findByUserIdAndStartTimeBetweenOrderByStartTimeAsc(
            Long userId, LocalDateTime start, LocalDateTime end);

    Optional<HealthCanonicalRecord> findByUserIdAndMetricTypeAndStartTimeAndEndTime(
            Long userId, String metricType, LocalDateTime startTime, LocalDateTime endTime);
}
