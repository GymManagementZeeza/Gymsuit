package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {

    List<Trainer> findAllByGymId(Long gymId);

    Optional<Trainer> findByIdAndGymId(Long id, Long gymId);

    long countByGymId(Long gymId);
}
