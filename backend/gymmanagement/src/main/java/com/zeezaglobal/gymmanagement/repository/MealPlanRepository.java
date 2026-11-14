package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    Optional<MealPlan> findByNutritionistIdAndMemberIdAndPlanDate(Long nutritionistUserId, Long memberId, LocalDate planDate);

    List<MealPlan> findTop10ByNutritionistIdAndMemberIdOrderByPlanDateDesc(Long nutritionistUserId, Long memberId);

    /** The client's plan for today, or the most recent earlier one. */
    Optional<MealPlan> findFirstByMemberIdAndPlanDateLessThanEqualOrderByPlanDateDescUpdatedAtDesc(Long memberId, LocalDate date);

    void deleteAllByMemberId(Long memberId);

    void deleteAllByNutritionistId(Long nutritionistUserId);

    void deleteAllByNutritionistIdAndMemberId(Long nutritionistUserId, Long memberId);
}
