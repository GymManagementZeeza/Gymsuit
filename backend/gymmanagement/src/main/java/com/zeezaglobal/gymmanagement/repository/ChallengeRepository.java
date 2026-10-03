package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    Optional<Challenge> findByInviteCodeIgnoreCase(String inviteCode);
}
