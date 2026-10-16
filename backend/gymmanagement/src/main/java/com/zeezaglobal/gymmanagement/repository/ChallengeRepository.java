package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.Challenge;
import com.zeezaglobal.gymmanagement.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    Optional<Challenge> findByInviteCodeIgnoreCase(String inviteCode);

    /** Challenges a user created (used to clear the creator on account deletion). */
    List<Challenge> findAllByCreatedBy(User user);
}
