package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDate;

public record DailyProgressEntryDto(
        LocalDate date,
        double value
) {
}
