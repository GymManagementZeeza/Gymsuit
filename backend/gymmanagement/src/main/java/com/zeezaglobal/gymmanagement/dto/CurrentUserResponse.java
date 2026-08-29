package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.ManagerAccessScope;

import java.util.Set;

public record CurrentUserResponse(
        Long userId,
        String email,
        String displayName,
        String role,
        Long gymId,
        Long trainerId,
        Long memberId,
        Long managerId,
        Set<ManagerAccessScope> managerScopes
) {
}
