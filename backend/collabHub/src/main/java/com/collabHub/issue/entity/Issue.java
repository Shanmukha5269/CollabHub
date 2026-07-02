package com.collabHub.issue.entity;

import com.collabHub.project.entity.Project;
import com.collabHub.sprint.entity.Sprint;
import com.collabHub.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "issues")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable issue identifier — e.g. "COLL-1", "COLL-2".
     * Built from project.projectKey + "-" + project.issueCounter.
     * Unique across the entire table (globally unique by construction).
     */
    @Column(nullable = false, unique = true, length = 20)
    private String issueKey;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Current lifecycle status.
     * Defaults to TODO on creation — same as how isEdited defaults to false in Message.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IssueStatus status = IssueStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IssuePriority priority = IssuePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IssueType type = IssueType.TASK;

    /**
     * The project this issue belongs to.
     * Mirrors how Message belongs to a Channel.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * User who created the issue.
     * Mirrors Message.sender — always set, never null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    /**
     * User assigned to work on this issue.
     * Nullable — an issue can exist in the backlog with no assignee.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    /**
     * Sprint this issue belongs to.
     * Nullable — null means the issue is in the backlog (not part of any sprint).
     * Set when the issue is added to a sprint via SprintService.addIssueToSprint().
     * Cleared back to null when a sprint completes and the issue wasn't finished.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    /**
     * Optional due date for the issue.
     * Uses LocalDate (date only, no time) — same as how Jira stores due dates.
     */
    @Column
    private LocalDate dueDate;

    /**
     * Integer position used for ordering on the board.
     * Stored with gaps (1000, 2000, 3000) so items can be inserted between
     * without renumbering. Phase 3 (drag-and-drop board) will use this.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
