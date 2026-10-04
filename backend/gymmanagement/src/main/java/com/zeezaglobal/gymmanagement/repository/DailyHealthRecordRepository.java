package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.DailyHealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyHealthRecordRepository extends JpaRepository<DailyHealthRecord, Long> {

    Optional<DailyHealthRecord> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);

    List<DailyHealthRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT r FROM DailyHealthRecord r WHERE r.user.id = :userId AND r.updatedAt > :since ORDER BY r.recordDate ASC")
    List<DailyHealthRecord> findUpdatedSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    List<DailyHealthRecord> findByUserIdOrderByRecordDateDesc(Long userId);
}
