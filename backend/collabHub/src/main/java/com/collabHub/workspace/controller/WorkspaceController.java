package com.collabHub.workspace.controller;

import com.collabHub.common.util.SecurityUtil;
import com.collabHub.workspace.dto.CreateWorkspaceRequestDTO;
import com.collabHub.workspace.dto.TransferOwnershipDTO;
import com.collabHub.workspace.dto.WorkspaceResponseDTO;
import com.collabHub.workspace.dto.AddWorkspaceMemberDTO;
import com.collabHub.workspace.dto.WorkspaceMemberDTO;
import com.collabHub.workspace.service.WorkspaceService;
import com.collabHub.workspace.service.WorkspaceMemberService;
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
    private final WorkspaceMemberService workspaceMemberService;

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
     * Delete workspace
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

    
    // WORKSPACE MEMBER MANAGEMENT 

    /**
     * Add a member to a workspace
     * 
     * Only the workspace owner can add members
     * 
     * @param workspaceId workspace ID
     * @param request member details to add
     * @return added member details
     */
    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<WorkspaceMemberDTO> addMember(
            @PathVariable Long workspaceId,
            @Valid @RequestBody AddWorkspaceMemberDTO request) {
        log.info("Request to add member {} to workspace {}", request.getUserId(), workspaceId);
        
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        WorkspaceMemberDTO member = workspaceMemberService.addMember(workspaceId, request, currentUserEmail);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    /**
     * Get all members of a workspace
     * 
     * Any member of the workspace can view other members
     * 
     * @param workspaceId workspace ID
     * @return list of workspace members
     */
    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<List<WorkspaceMemberDTO>> getWorkspaceMembers(@PathVariable Long workspaceId) {
        log.info("Fetching members for workspace {}", workspaceId);
        
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        List<WorkspaceMemberDTO> members = workspaceMemberService.getWorkspaceMembers(workspaceId, currentUserEmail);
        
        return ResponseEntity.ok(members);
    }

    /**
     * Get a specific member's details
     * 
     * @param workspaceId workspace ID
     * @param userId user ID to get details of
     * @return member details
     */
    @GetMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<WorkspaceMemberDTO> getMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId) {
        log.info("Fetching member {} details in workspace {}", userId, workspaceId);
        
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        WorkspaceMemberDTO member = workspaceMemberService.getMember(workspaceId, userId, currentUserEmail);
        
        return ResponseEntity.ok(member);
    }

    /**
     * - Promote MEMBER to OWNER (transfer ownership)
     * - Demote OWNER to MEMBER
     */
    @PutMapping("/{workspaceId}/transfer-ownership")
    public ResponseEntity<WorkspaceMemberDTO> transferOwnership(
            @PathVariable Long workspaceId,
            @Valid @RequestBody TransferOwnershipDTO request) {

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();

        return ResponseEntity.ok(
            workspaceMemberService.transferOwnership(workspaceId, request, currentUserEmail)
        );
    }

    /**
     * Remove a member from a workspace
     * 
     * Only the workspace owner can remove members
     * 
     * @param workspaceId workspace ID
     * @param userId user ID to remove
     * @return 204 No Content
     */
    @DeleteMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId) {
        log.info("Request to remove member {} from workspace {}", userId, workspaceId);
        
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        workspaceMemberService.removeMember(workspaceId, userId, currentUserEmail);
        
        return ResponseEntity.noContent().build();
    }
}
