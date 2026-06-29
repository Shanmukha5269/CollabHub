package com.collabHub.issue.dto;

import com.collabHub.issue.entity.IssuePriority;
import com.collabHub.issue.entity.IssueStatus;
import com.collabHub.issue.entity.IssueType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueResponseDTO {

    private Long id;

    /** Human-readable key like "COLL-1". */
    private String issueKey;

    private String title;
    private String description;
    private IssueStatus status;
    private IssuePriority priority;
    private IssueType type;

    private Long projectId;
    private String projectName;
    private String projectKey;

    private UserMinimalDTO reporter;

    /** Null if unassigned. */
    private UserMinimalDTO assignee;

    private LocalDate dueDate;
    private Integer position;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Minimal user info embedded in the response.
     * Mirrors MessageResponseDTO.UserMinimalDTO exactly —
     * same static inner class pattern used throughout the codebase.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserMinimalDTO {
        private Long id;
        private String name;
        private String email;
    }
}
