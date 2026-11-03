package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record FoodIntakeSyncRequest(
        @Valid List<Entry> entries
) {
    public record Entry(
            @NotBlank String externalId,
            @NotNull LocalDate recordDate,
            String meal,
            String dishName,
            Integer calories,
            Double proteinG,
            Double carbsG,
            Double fatG
    ) {
    }
}
