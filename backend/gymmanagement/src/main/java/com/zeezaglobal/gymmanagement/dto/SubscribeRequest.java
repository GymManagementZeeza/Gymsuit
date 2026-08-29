package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.NotNull;

public record SubscribeRequest(
        @NotNull Long planId
) {
}
