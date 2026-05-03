package com.collabHub.admin.dto;

import lombok.*;


/**
 * Contains aggregate statistics about workspace and members
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceStatisticsDTO {
    
    private Long totalWorkspaces;
    private Long totalMembers;
    private Double averageMembersPerWorkspace;
}
