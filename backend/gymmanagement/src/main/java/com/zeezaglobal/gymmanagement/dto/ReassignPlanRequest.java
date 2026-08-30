package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.NotNull;

public record ReassignPlanRequest(
        @NotNull Long replacementPlanId
) {
}
