package com.zeezaglobal.gymmanagement.entity;

public enum ManagerAccessScope {
    /** Pay rates, payroll, and compensation across the gym — owner-only unless granted. */
    FINANCE,
    /** Gym profile/settings, and managing the team roster (inviting/removing other managers). */
    SETTINGS,
    /** Adding, editing, and removing trainers. */
    TRAINERS
}
