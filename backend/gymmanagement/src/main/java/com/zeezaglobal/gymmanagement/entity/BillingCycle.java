package com.zeezaglobal.gymmanagement.entity;

import java.time.LocalDate;

public enum BillingCycle {
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    QUARTERLY,
    SEMI_ANNUAL,
    ANNUAL;

    /** The end date of a billing period that starts on {@code start}, given this cycle's length. */
    public LocalDate periodEnd(LocalDate start) {
        return switch (this) {
            case WEEKLY -> start.plusWeeks(1);
            case BIWEEKLY -> start.plusWeeks(2);
            case MONTHLY -> start.plusMonths(1);
            case QUARTERLY -> start.plusMonths(3);
            case SEMI_ANNUAL -> start.plusMonths(6);
            case ANNUAL -> start.plusYears(1);
        };
    }
}
