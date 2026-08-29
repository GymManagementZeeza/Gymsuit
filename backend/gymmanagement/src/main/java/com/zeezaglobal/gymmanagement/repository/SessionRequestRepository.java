package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.SessionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRequestRepository extends JpaRepository<SessionRequest, Long> {

    List<SessionRequest> findAllByGymId(Long gymId);

    List<SessionRequest> findAllByGymIdAndTrainerId(Long gymId, Long trainerId);

    List<SessionRequest> findAllByGymIdAndMemberId(Long gymId, Long memberId);

    Optional<SessionRequest> findByIdAndGymId(Long id, Long gymId);

    void deleteAllByMemberId(Long memberId);
}
