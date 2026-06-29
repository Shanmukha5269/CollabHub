package com.collabHub.issue.entity;

/**
 * Priority level of an issue.
 * Stored as a STRING in the DB via @Enumerated(EnumType.STRING).
 *
 * Used to sort issues on the board and in the backlog.
 * Ordering: LOW -> MEDIUM -> HIGH -> CRITICAL
 */
public enum IssuePriority {

    /** Nice to have, no urgency. */
    LOW,

    /** Standard issue — should be addressed in the current or next sprint. */
    MEDIUM,

    /** Blocks progress; needs attention soon. */
    HIGH,

    /** Production-breaking; must be fixed immediately. */
    CRITICAL
}
