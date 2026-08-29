package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.MemberPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberPaymentRepository extends JpaRepository<MemberPayment, Long> {

    List<MemberPayment> findAllByGymIdOrderByCreatedAtDesc(Long gymId);

    List<MemberPayment> findAllByGymIdAndMemberIdOrderByCreatedAtDesc(Long gymId, Long memberId);

    Optional<MemberPayment> findByIdAndGymId(Long id, Long gymId);

    void deleteAllByMemberId(Long memberId);
}
