package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.FoodIntakeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FoodIntakeRepository extends JpaRepository<FoodIntakeRecord, Long> {

    Optional<FoodIntakeRecord> findByUserIdAndExternalId(Long userId, String externalId);

    List<FoodIntakeRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
            Long userId, LocalDate startDate, LocalDate endDate);

    boolean existsByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
