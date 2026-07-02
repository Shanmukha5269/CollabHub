package com.collabHub.sprint.entity;

import com.collabHub.project.entity.Project;
import com.collabHub.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sprints")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * Optional goal statement — what should be achieved by the end of this sprint.
     * e.g. "Ship the authentication flow and basic dashboard"
     */
    @Column(length = 1000)
    private String goal;

    /**
     * Current lifecycle status.
     * Defaults to PLANNING — the team sets up the sprint before starting it.
     * Mirrors @Builder.Default pattern used in Issue entity.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SprintStatus status = SprintStatus.PLANNING;

    /**
     * The project this sprint belongs to.
     * Mirrors how Issue belongs to a Project.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * User who created this sprint.
     * Mirrors Issue.reporter pattern.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    /**
     * Planned start date — set when creating the sprint.
     * Actual start is recorded via startedAt when the sprint is explicitly started.
     */
    @Column
    private LocalDate startDate;

    /**
     * Planned end date — the sprint deadline.
     */
    @Column
    private LocalDate endDate;

    /**
     * Timestamp of when startSprint() was actually called.
     * Nullable — null until the sprint transitions from PLANNING → ACTIVE.
     */
    @Column
    private LocalDateTime startedAt;

    /**
     * Timestamp of when completeSprint() was called.
     * Nullable — null until the sprint transitions from ACTIVE → COMPLETED.
     */
    @Column
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
