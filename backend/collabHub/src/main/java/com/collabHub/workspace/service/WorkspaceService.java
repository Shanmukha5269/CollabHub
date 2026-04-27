package com.collabHub.workspace.service;

import com.collabHub.workspace.dto.CreateWorkspaceRequestDTO;
import com.collabHub.workspace.dto.WorkspaceResponseDTO;

public interface WorkspaceService {

    /**
     * Create a new workspace
     * @param request workspace details
     * @param ownerEmail email of the user creating the workspace
     * @return created workspace response
     */
    WorkspaceResponseDTO createWorkspace(CreateWorkspaceRequestDTO request, String ownerEmail);
}
