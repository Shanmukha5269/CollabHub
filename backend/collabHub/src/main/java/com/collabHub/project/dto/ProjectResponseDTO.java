package com.collabHub.project.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponseDTO {

    private Long id;
    private String name;
    private String description;
    private String projectKey;

    private Long workspaceId;
    private String workspaceName;

    private Long creatorId;
    private String creatorName;
    private String creatorEmail;

    /** Nullable — a project may have no assigned lead. */
    private Long leadId;
    private String leadName;
    private String leadEmail;

    private Long workspaceOwnerId;
    private String workspaceOwnerName;
    private String workspaceOwnerEmail;

    /** Current issue count — derived from issueCounter on the entity. */
    private Integer issueCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
