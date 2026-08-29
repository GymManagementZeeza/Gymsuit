package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.TrainingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {

    List<TrainingSession> findAllByGymId(Long gymId);

    Optional<TrainingSession> findByIdAndGymId(Long id, Long gymId);

    List<TrainingSession> findAllByTrainerId(Long trainerId);
}
