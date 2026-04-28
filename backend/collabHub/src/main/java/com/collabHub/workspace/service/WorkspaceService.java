package com.collabHub.workspace.service;

import com.collabHub.workspace.dto.CreateWorkspaceRequestDTO;
import com.collabHub.workspace.dto.WorkspaceResponseDTO;
import java.util.List;

public interface WorkspaceService {

    /**
     * Create a new workspace
     * @param request workspace details
     * @param ownerEmail email of the user creating the workspace
     * @return created workspace response
     */
    WorkspaceResponseDTO createWorkspace(CreateWorkspaceRequestDTO request, String ownerEmail);

    /**
     * Get all workspaces owned by the current user
     * @param ownerEmail email of the user
     * @return list of workspaces owned by the user
     */
    List<WorkspaceResponseDTO> getUserWorkspaces(String ownerEmail);

    /**
     * Get workspace details by ID
     * @param workspaceId workspace ID
     * @param ownerEmail email of the user requesting
     * @return workspace details
     */
    WorkspaceResponseDTO getWorkspaceById(Long workspaceId, String ownerEmail);

    /**
     * Update workspace details
     * Only the owner can update their workspace
     * @param workspaceId workspace ID to update
     * @param request updated workspace details
     * @param ownerEmail email of the user requesting
     * @return updated workspace response
     */
    WorkspaceResponseDTO updateWorkspace(Long workspaceId, CreateWorkspaceRequestDTO request, String ownerEmail);

    /**
     * Delete workspace (soft delete)
     * Only the owner can delete their workspace
     * @param workspaceId workspace ID to delete
     * @param ownerEmail email of the user requesting
     */
    void deleteWorkspace(Long workspaceId, String ownerEmail);
}
