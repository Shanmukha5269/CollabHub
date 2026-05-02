package com.collabHub.workspace.dto;

import com.collabHub.workspace.entity.WorkspaceRole;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for workspace member response
 * 
 * Contains member information including user details and role
 * Used in response for GET /api/workspaces/{id}/members
 * 
 * Example:
 * {
 *   "id": 10,
 *   "userId": 5,
 *   "userName": "John Doe",
 *   "userEmail": "john@example.com",
 *   "role": "OWNER",
 *   "canManageMembers": true,
 *   "joinedAt": "2026-04-30T10:15:00",
 *   "isActive": true
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceMemberDTO {

    /**
     * WorkspaceMember ID (the relationship record ID)
     */
    private Long id;

    /**
     * ID of the user
     */
    private Long userId;

    /**
     * Name of the user
     */
    private String userName;

    /**
     * Email of the user
     */
    private String userEmail;

    /**
     * Role of the user in this workspace
     * Values: MEMBER, OWNER
     */
    private WorkspaceRole role;

    /**
     * Whether this member can manage other members in this workspace
     */
    private Boolean canManageMembers;

    /**
     * Timestamp when the user joined this workspace
     */
    private LocalDateTime joinedAt;

    /**
     * Whether the membership is active (not removed)
     */
    @Builder.Default
    private Boolean isActive = true;
}
