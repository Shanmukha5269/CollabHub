package com.collabHub.sprint.dto;

import com.collabHub.sprint.entity.SprintStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SprintResponseDTO {

    private Long id;
    private String name;
    private String goal;
    private SprintStatus status;

    private Long projectId;
    private String projectName;
    private String projectKey;

    private Long creatorId;
    private String creatorName;
    private String creatorEmail;

    private LocalDate startDate;
    private LocalDate endDate;

    /** Actual timestamp when the sprint was started — null if not yet started. */
    private LocalDateTime startedAt;

    /** Actual timestamp when the sprint was completed — null if not yet completed. */
    private LocalDateTime completedAt;

    /** How many issues are currently in this sprint. */
    private Integer issueCount;

    /** How many of those issues are DONE. */
    private Integer completedIssueCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
