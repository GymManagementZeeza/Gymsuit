package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    List<CheckIn> findAllByGymIdAndMemberIdOrderByCheckInTimeDesc(Long gymId, Long memberId);

    List<CheckIn> findAllByGymIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(Long gymId);

    List<CheckIn> findAllByGymIdAndCheckInTimeAfterOrderByCheckInTimeAsc(Long gymId, LocalDateTime after);

    Optional<CheckIn> findByGymIdAndMemberIdAndCheckOutTimeIsNull(Long gymId, Long memberId);

    Optional<CheckIn> findByIdAndGymId(Long id, Long gymId);

    List<CheckIn> findAllByCheckOutTimeIsNullAndCheckInTimeBefore(LocalDateTime cutoff);

    void deleteAllByMemberId(Long memberId);
}
