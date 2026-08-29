package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
}
