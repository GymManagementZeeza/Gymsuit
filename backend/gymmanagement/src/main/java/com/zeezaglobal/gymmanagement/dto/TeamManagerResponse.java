package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.Manager;
import com.zeezaglobal.gymmanagement.entity.ManagerAccessScope;

import java.util.Set;

public record TeamManagerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Set<ManagerAccessScope> scopes
) {
    public static TeamManagerResponse fromEntity(Manager manager, Set<ManagerAccessScope> scopes) {
        return new TeamManagerResponse(
                manager.getId(),
                manager.getFirstName(),
                manager.getLastName(),
                manager.getEmail(),
                manager.getPhone(),
                scopes
        );
    }
}
