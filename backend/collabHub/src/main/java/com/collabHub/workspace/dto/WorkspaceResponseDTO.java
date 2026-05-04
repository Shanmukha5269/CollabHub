package com.collabHub.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceResponseDTO {

    private Long id;

    private String name;

    private String description;

    /**
     * Owner's basic info - minimal fields without null values
     */
    private UserBasicInfoDTO owner;

    /**
     * List of all active members in this workspace
     * Includes the owner and all invited members
     */
    private List<WorkspaceMemberDTO> members;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Boolean suspended;

    private LocalDateTime suspendedAt;

    private String suspensionReason;
}
