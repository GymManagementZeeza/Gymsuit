package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.WorkoutRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutRecordRepository extends JpaRepository<WorkoutRecord, Long> {

    Optional<WorkoutRecord> findByUserIdAndExternalId(Long userId, String externalId);

    List<WorkoutRecord> findByUserIdAndStartTimeBetweenOrderByStartTimeDesc(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT w FROM WorkoutRecord w WHERE w.user.id = :userId AND w.updatedAt > :since ORDER BY w.startTime DESC")
    List<WorkoutRecord> findUpdatedSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    List<WorkoutRecord> findByUserIdOrderByStartTimeDesc(Long userId);

    /** Ids of users with any workouts starting in the window — the health report audience. */
    @Query("SELECT DISTINCT w.user.id FROM WorkoutRecord w WHERE w.startTime BETWEEN :start AND :end")
    List<Long> findUserIdsWithWorkouts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    void deleteAllByUserId(Long userId);
}
