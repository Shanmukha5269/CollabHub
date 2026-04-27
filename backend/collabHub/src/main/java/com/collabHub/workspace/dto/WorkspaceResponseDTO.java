package com.collabHub.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
