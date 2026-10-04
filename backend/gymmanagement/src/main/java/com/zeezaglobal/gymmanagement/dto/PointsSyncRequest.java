package com.zeezaglobal.gymmanagement.dto;

import java.util.List;

public record PointsSyncRequest(
        String deviceId,
        List<PointsTransactionDto> transactions
) {}
