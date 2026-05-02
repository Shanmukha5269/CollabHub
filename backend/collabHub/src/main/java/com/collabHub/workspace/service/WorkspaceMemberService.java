package com.collabHub.workspace.service;

import com.collabHub.workspace.dto.AddWorkspaceMemberDTO;
import com.collabHub.workspace.dto.TransferOwnershipDTO;
import com.collabHub.workspace.dto.WorkspaceMemberDTO;

import java.util.List;

/**
 * Service interface for Workspace Member operations
 */
public interface WorkspaceMemberService {

    /**
     * Add a member to a workspace
     * Only the workspace owner can add members
     * 
     * @param workspaceId workspace ID
     * @param request member details to add
     * @param requesterEmail email of the user requesting (must be owner)
     * @return the newly added member details
     */
    WorkspaceMemberDTO addMember(Long workspaceId, AddWorkspaceMemberDTO request, String requesterEmail);

    /**
     * Get all active members of a workspace
     * Any member of the workspace can view other members
     * 
     * @param workspaceId workspace ID
     * @param requesterEmail email of the user requesting
     * @return list of active members
     */
    List<WorkspaceMemberDTO> getWorkspaceMembers(Long workspaceId, String requesterEmail);

    /**
     * Get a specific member's details
     * 
     * @param workspaceId workspace ID
     * @param userId user ID to get details of
     * @param requesterEmail email of the user requesting
     * @return member details
     */
    WorkspaceMemberDTO getMember(Long workspaceId, Long userId, String requesterEmail);

    /**
     * Transfers ownership in a workspace
     * Only the workspace owner can perform this action
     * @param workspaceId workspace ID
     * @param userId user ID to update
     * @param request new role details
     * @param requesterEmail email of the user requesting (must be owner)
     * @return updated member details
     */
    WorkspaceMemberDTO transferOwnership(Long workspaceId, TransferOwnershipDTO request, String requesterEmail);

    /**
     * Remove a member from a workspace (soft delete)
     * Only the workspace owner can remove members
     * 
     * @param workspaceId workspace ID
     * @param userId user ID to remove
     * @param requesterEmail email of the user requesting (must be owner)
     */
    void removeMember(Long workspaceId, Long userId, String requesterEmail);

    /**
     * Check if a user is a member of a workspace
     * 
     * @param workspaceId workspace ID
     * @param userId user ID to check
     * @return true if user is active member, false otherwise
     */
    boolean isMember(Long workspaceId, Long userId);

    /**
     * Check if a user is the owner of a workspace
     * 
     * @param workspaceId workspace ID
     * @param userId user ID to check
     * @return true if user is workspace owner, false otherwise
     */
    boolean isOwner(Long workspaceId, Long userId);

    /**
     * Get count of active members in a workspace
     * 
     * @param workspaceId workspace ID
     * @return number of active members
     */
    long getMembersCount(Long workspaceId);
}
