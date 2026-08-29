package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByMemberId(Long memberId);

    Optional<User> findByTrainerId(Long trainerId);

    boolean existsByRole(Role role);
}
