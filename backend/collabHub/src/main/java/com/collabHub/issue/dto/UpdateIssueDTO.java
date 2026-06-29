package com.collabHub.issue.dto;

import com.collabHub.issue.entity.IssuePriority;
import com.collabHub.issue.entity.IssueType;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateIssueDTO {

    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private IssuePriority priority;

    private IssueType type;

    /** Pass null to keep the current assignee, pass a userId to change it. */
    private Long assigneeId;

    private LocalDate dueDate;
}
