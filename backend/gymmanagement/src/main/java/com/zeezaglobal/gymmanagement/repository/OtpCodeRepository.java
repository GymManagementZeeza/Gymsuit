package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findTopByEmailAndConsumedFalseOrderByIdDesc(String email);
}
