package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.GymTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GymTransactionRepository extends JpaRepository<GymTransaction, Long> {

    List<GymTransaction> findAllByGymIdOrderByOccurredOnDescCreatedAtDesc(Long gymId);

    List<GymTransaction> findAllByMemberId(Long memberId);
}
