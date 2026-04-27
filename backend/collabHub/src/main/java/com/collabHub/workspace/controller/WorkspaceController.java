package com.collabHub.workspace.controller;

import com.collabHub.common.util.SecurityUtil;
import com.collabHub.workspace.dto.CreateWorkspaceRequestDTO;
import com.collabHub.workspace.dto.WorkspaceResponseDTO;
import com.collabHub.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Workspace Controller
 * Handles workspace management endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    /**
     * Create a new workspace
     * POST /api/workspaces
     * 
     * The authenticated user becomes the owner of the workspace
     * 
     * @param request workspace creation details
     * @return created workspace response
     */
    @PostMapping
    public ResponseEntity<WorkspaceResponseDTO> createWorkspace(@Valid @RequestBody CreateWorkspaceRequestDTO request) {
        log.info("Workspace creation request: {}", request.getName());
        
        // Get current logged-in user's email from JWT token
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        
        // Create workspace
        WorkspaceResponseDTO response = workspaceService.createWorkspace(request, currentUserEmail);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
