package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.ManagerAccessScope;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record InviteManagerRequest(
        @NotBlank @Email String email,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        Set<ManagerAccessScope> scopes
) {
}
