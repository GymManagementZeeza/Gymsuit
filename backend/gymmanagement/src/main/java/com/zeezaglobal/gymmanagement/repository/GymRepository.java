package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.Gym;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GymRepository extends JpaRepository<Gym, Long> {

    List<Gym> findByNameContainingIgnoreCase(String name);
}
