package com.zeezaglobal.gymmanagement.dto;

import java.util.List;

public record DailyProgressRequest(
        List<DailyProgressEntryDto> entries
) {
}
