package com.zeezaglobal.gymmanagement.dto;

/** trainerId null unassigns the member's current trainer. */
public record MemberTrainerRequest(Long trainerId) {
}
