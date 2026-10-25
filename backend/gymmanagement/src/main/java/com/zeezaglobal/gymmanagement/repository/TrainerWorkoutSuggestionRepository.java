package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.TrainerWorkoutSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TrainerWorkoutSuggestionRepository extends JpaRepository<TrainerWorkoutSuggestion, Long> {

    Optional<TrainerWorkoutSuggestion> findByMemberIdAndSuggestionDate(Long memberId, LocalDate suggestionDate);
}
