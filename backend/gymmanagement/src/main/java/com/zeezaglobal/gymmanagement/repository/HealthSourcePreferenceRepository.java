package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.HealthSourcePreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthSourcePreferenceRepository extends JpaRepository<HealthSourcePreference, Long> {
    List<HealthSourcePreference> findByUserId(Long userId);
    Optional<HealthSourcePreference> findByUserIdAndMetricType(Long userId, String metricType);
}
