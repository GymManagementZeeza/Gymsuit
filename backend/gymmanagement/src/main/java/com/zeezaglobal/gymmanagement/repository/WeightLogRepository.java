package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.WeightLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeightLogRepository extends JpaRepository<WeightLog, Long> {

    List<WeightLog> findAllByMemberIdOrderByRecordedAtAsc(Long memberId);

    void deleteAllByMemberId(Long memberId);
}
