package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.ChallengeDailyProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ChallengeDailyProgressRepository extends JpaRepository<ChallengeDailyProgress, Long> {

    List<ChallengeDailyProgress> findByChallengeId(Long challengeId);

    List<ChallengeDailyProgress> findByChallengeIdAndUserIdOrderByDateAsc(Long challengeId, Long userId);

    Optional<ChallengeDailyProgress> findByChallengeIdAndUserIdAndDate(
            Long challengeId, Long userId, LocalDate date);

    void deleteAllByUserId(Long userId);
}
