package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.ManagerCompensation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagerCompensationRepository extends JpaRepository<ManagerCompensation, Long> {

    Optional<ManagerCompensation> findByGymIdAndManagerId(Long gymId, Long managerId);
}
