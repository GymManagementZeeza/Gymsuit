package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.NutritionistInvite;
import com.zeezaglobal.gymmanagement.entity.TrainerInviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NutritionistInviteRepository extends JpaRepository<NutritionistInvite, Long> {

    Optional<NutritionistInvite> findByToken(String token);

    Optional<NutritionistInvite> findByNutritionistIdAndEmailAndStatus(
            Long nutritionistUserId, String email, TrainerInviteStatus status);

    List<NutritionistInvite> findAllByNutritionistIdOrderByCreatedAtDesc(Long nutritionistUserId);

    Optional<NutritionistInvite> findByIdAndNutritionistId(Long id, Long nutritionistUserId);

    void deleteAllByNutritionistId(Long nutritionistUserId);
}
