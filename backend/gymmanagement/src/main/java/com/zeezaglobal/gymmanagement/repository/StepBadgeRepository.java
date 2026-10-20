package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.StepBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StepBadgeRepository extends JpaRepository<StepBadge, Long> {

    boolean existsByUserIdAndThresholdAndBadgeDate(Long userId, int threshold, LocalDate badgeDate);

    List<StepBadge> findByUserIdOrderByAwardedAtDesc(Long userId);

    void deleteAllByUserId(Long userId);
}
