package com.collabHub.issue.entity;

/**
 * Lifecycle status of an issue — mirrors Jira's default columns.
 * Stored as a STRING in the DB via @Enumerated(EnumType.STRING).
 *
 * These also map directly to board columns in Phase 3 (Board feature).
 * Ordering: TODO -> IN_PROGRESS -> IN_REVIEW -> DONE
 */
public enum IssueStatus {

    /** Issue is created but no one has started working on it yet. */
    TODO,

    /** Someone is actively working on this issue. */
    IN_PROGRESS,

    /** Work is done; waiting for review or testing. */
    IN_REVIEW,

    /** Issue is fully resolved and closed. */
    DONE
}
