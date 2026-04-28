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

import java.util.List;

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

    /**
     * Returns a list of all workspaces owned by the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<WorkspaceResponseDTO>> getUserWorkspaces() {
        log.info("Fetching user workspaces request");
        
        // Get current logged-in user's email from JWT token
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        
        // Get all workspaces for the user
        List<WorkspaceResponseDTO> workspaces = workspaceService.getUserWorkspaces(currentUserEmail);
        
        return ResponseEntity.ok(workspaces);
    }

    /**
     * Get workspace by ID
     * @param id workspace ID
     * @return workspace details
     */
    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponseDTO> getWorkspaceById(@PathVariable Long id) {
        log.info("Fetching workspace details for ID: {}", id);
        
        // Get current logged-in user's email from JWT token
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        
        // Get workspace details
        WorkspaceResponseDTO workspace = workspaceService.getWorkspaceById(id, currentUserEmail);
        
        return ResponseEntity.ok(workspace);
    }

    /**
     * Update workspace details
     * 
     * Only the owner can update their workspace
     * 
     * @param id workspace ID to update
     * @param request updated workspace details
     * @return updated workspace response
     */
    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponseDTO> updateWorkspace(
            @PathVariable Long id,
            @Valid @RequestBody CreateWorkspaceRequestDTO request) {
        log.info("Workspace update request for ID: {}", id);
        
        // Get current logged-in user's email from JWT token
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        
        // Update workspace
        WorkspaceResponseDTO response = workspaceService.updateWorkspace(id, request, currentUserEmail);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Delete workspace (soft delete)
     * 
     * Only the owner can delete their workspace
     * 
     * @param id workspace ID to delete
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable Long id) {
        log.info("Workspace deletion request for ID: {}", id);
        
        // Get current logged-in user's email from JWT token
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        
        // Delete workspace
        workspaceService.deleteWorkspace(id, currentUserEmail);
        
        return ResponseEntity.noContent().build();
    }
}
