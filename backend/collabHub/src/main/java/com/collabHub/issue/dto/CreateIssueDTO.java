package com.collabHub.issue.dto;

import com.collabHub.issue.entity.IssuePriority;
import com.collabHub.issue.entity.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateIssueDTO {

    @NotBlank(message = "Issue title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    /**
     * Defaults to TASK if not provided — same default as the entity.
     * Frontend can omit this field for the simplest issue creation flow.
     */
    private IssueType type;

    /**
     * Defaults to MEDIUM if not provided.
     */
    private IssuePriority priority;

    /** Optional: assign to a workspace member at creation time. */
    private Long assigneeId;

    /** Optional due date. */
    private LocalDate dueDate;
}
