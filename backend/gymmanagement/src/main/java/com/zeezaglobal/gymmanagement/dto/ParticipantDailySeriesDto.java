package com.zeezaglobal.gymmanagement.dto;

import java.util.List;

public record ParticipantDailySeriesDto(
        Long userId,
        String displayName,
        List<DailyProgressEntryDto> entries
) {
}
