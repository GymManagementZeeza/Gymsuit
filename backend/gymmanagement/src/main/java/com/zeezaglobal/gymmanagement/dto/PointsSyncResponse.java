package com.zeezaglobal.gymmanagement.dto;

import java.util.List;

public record PointsSyncResponse(
        int balance,
        List<PointsTransactionDto> transactions
) {}
