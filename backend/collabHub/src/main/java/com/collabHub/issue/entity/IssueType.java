package com.collabHub.issue.entity;

/**
 * Category / type of an issue.
 * Stored as a STRING in the DB via @Enumerated(EnumType.STRING).
 *
 * These are the same three types Jira ships with by default.
 */
public enum IssueType {

    /** A piece of work to be done — the most generic type. */
    TASK,

    /** A defect or unintended behaviour that needs to be fixed. */
    BUG,

    /** A user-facing feature described from the end-user's perspective. */
    STORY
}
