package com.collabHub.sprint.entity;

/**
 * Lifecycle status of a sprint.
 * Stored as STRING in DB via @Enumerated(EnumType.STRING).
 *
 * State machine — only one valid flow:
 *   PLANNING → ACTIVE → COMPLETED
 *
 * A project can only have ONE sprint in ACTIVE state at any time.
 * This is enforced in SprintServiceImpl.startSprint().
 */
public enum SprintStatus {

    /** Sprint is being planned — issues can be added but work hasn't started. */
    PLANNING,

    /** Sprint is running — team is actively working on the issues. */
    ACTIVE,

    /** Sprint is closed — incomplete issues moved back to backlog. */
    COMPLETED
}
