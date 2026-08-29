package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.ManagerAccessScope;

import java.util.Set;

public record ManagerAccessUpdateRequest(Set<ManagerAccessScope> scopes) {
}
