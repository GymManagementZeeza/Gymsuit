package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findAllByGymIdOrderByPeriodStartDesc(Long gymId);

    List<Payment> findAllByGymIdAndTrainerIdOrderByPeriodStartDesc(Long gymId, Long trainerId);

    List<Payment> findAllByGymIdAndManagerIdOrderByPeriodStartDesc(Long gymId, Long managerId);

    Optional<Payment> findByIdAndGymId(Long id, Long gymId);
}
