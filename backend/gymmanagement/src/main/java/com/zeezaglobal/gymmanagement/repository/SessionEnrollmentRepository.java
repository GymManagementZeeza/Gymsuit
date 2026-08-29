package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.SessionEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionEnrollmentRepository extends JpaRepository<SessionEnrollment, Long> {

    List<SessionEnrollment> findAllBySessionId(Long sessionId);

    Optional<SessionEnrollment> findBySessionIdAndMemberId(Long sessionId, Long memberId);

    long countBySessionId(Long sessionId);

    void deleteAllByMemberId(Long memberId);
}
