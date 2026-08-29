package com.zeezaglobal.gymmanagement.entity;

public enum SubscriptionStatus {
    /** Assigned to a member but not yet paid for — doesn't count as their active plan. */
    PENDING,
    ACTIVE,
    PAUSED,
    CANCELLED,
    EXPIRED
}
