package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.TrainerInvite;
import com.zeezaglobal.gymmanagement.entity.TrainerInviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainerInviteRepository extends JpaRepository<TrainerInvite, Long> {

    Optional<TrainerInvite> findByToken(String token);

    Optional<TrainerInvite> findByGymIdAndTrainerIdAndEmailAndStatus(
            Long gymId, Long trainerId, String email, TrainerInviteStatus status);

    List<TrainerInvite> findAllByGymIdAndTrainerIdOrderByCreatedAtDesc(Long gymId, Long trainerId);

    Optional<TrainerInvite> findByIdAndGymIdAndTrainerId(Long id, Long gymId, Long trainerId);
}
