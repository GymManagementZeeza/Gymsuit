package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TrainerWorkoutSuggestionRequest(
        @Size(max = 500) String message,
        @NotEmpty @Valid List<ExerciseInput> exercises
) {
    public record ExerciseInput(
            @NotBlank @Size(max = 200) String exerciseName,
            @Min(1) @Max(50) int sets,
            @Min(1) @Max(1000) int reps,
            @DecimalMin("0.0") double weightKg
    ) {
    }
}
