package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.ChallengeParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChallengeParticipantRepository extends JpaRepository<ChallengeParticipant, Long> {

    List<ChallengeParticipant> findByChallengeId(Long challengeId);

    Optional<ChallengeParticipant> findByChallengeIdAndUserId(Long challengeId, Long userId);

    long countByChallengeId(Long challengeId);

    List<ChallengeParticipant> findByUserId(Long userId);
}
