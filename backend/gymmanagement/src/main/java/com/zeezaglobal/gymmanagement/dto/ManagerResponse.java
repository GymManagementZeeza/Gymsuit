package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.Manager;

public record ManagerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone
) {
    public static ManagerResponse fromEntity(Manager manager) {
        return new ManagerResponse(
                manager.getId(),
                manager.getFirstName(),
                manager.getLastName(),
                manager.getEmail(),
                manager.getPhone()
        );
    }
}
