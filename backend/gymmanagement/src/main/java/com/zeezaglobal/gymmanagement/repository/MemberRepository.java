package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findAllByGymId(Long gymId);

    Optional<Member> findByIdAndGymId(Long id, Long gymId);

    List<Member> findAllByTrainerId(Long trainerId);
}
