package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {

    List<MembershipPlan> findAllByGymId(Long gymId);

    Optional<MembershipPlan> findByIdAndGymId(Long id, Long gymId);

    long countByGymId(Long gymId);
}
