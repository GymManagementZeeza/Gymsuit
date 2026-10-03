package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.ChallengeInvite;
import com.zeezaglobal.gymmanagement.entity.ChallengeInviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChallengeInviteRepository extends JpaRepository<ChallengeInvite, Long> {

    Optional<ChallengeInvite> findByToken(String token);

    Optional<ChallengeInvite> findByChallengeIdAndEmailAndStatus(Long challengeId, String email,
                                                                 ChallengeInviteStatus status);
}
