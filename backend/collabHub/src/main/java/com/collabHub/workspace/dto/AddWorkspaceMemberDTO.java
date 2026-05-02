package com.collabHub.workspace.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO for adding/inviting a member to a workspace
 * 
 * Request body for POST /api/workspaces/{id}/members
 * 
 * Example:
 * {
 *   "userId": 5,
 *   "role": "MEMBER"
 * }
 * 
 * Note: New members are added as MEMBER by default
 *       To make someone an OWNER, use the role update endpoint after they join
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddWorkspaceMemberDTO {

    /**
     * ID of the user to add to the workspace
     */
    @NotNull(message = "User ID is required")
    private Long userId;

    /**
     * Role to assign to the user in this workspace
     * Default: MEMBER
     * Can be: MEMBER, OWNER
     */
    @Builder.Default
    private String role = "MEMBER";
}
