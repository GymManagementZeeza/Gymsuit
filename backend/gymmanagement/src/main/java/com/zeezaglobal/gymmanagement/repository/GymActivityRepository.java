package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.GymActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GymActivityRepository extends JpaRepository<GymActivity, Long> {

    List<GymActivity> findTop30ByGymIdOrderByCreatedAtDesc(Long gymId);
}
