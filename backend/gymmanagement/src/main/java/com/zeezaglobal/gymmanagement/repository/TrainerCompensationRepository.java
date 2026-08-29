package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.TrainerCompensation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrainerCompensationRepository extends JpaRepository<TrainerCompensation, Long> {

    Optional<TrainerCompensation> findByTrainerId(Long trainerId);
}
