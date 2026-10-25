package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDate;
import java.util.List;

public record TrainerWorkoutSuggestionResponse(
        Long id,
        LocalDate date,
        String trainerName,
        String message,
        List<ExerciseDto> exercises
) {
    public record ExerciseDto(
            String exerciseName,
            int sets,
            int reps,
            double weightKg
    ) {
    }
}
