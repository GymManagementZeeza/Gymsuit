package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.NutritionistClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NutritionistClientRepository extends JpaRepository<NutritionistClient, Long> {

    boolean existsByNutritionistIdAndMemberId(Long nutritionistUserId, Long memberId);

    List<NutritionistClient> findAllByNutritionistIdOrderByLinkedAtDesc(Long nutritionistUserId);

    Optional<NutritionistClient> findByNutritionistIdAndMemberId(Long nutritionistUserId, Long memberId);

    void deleteAllByNutritionistId(Long nutritionistUserId);

    void deleteAllByMemberId(Long memberId);
}
