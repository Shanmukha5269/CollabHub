package com.collabHub.admin.controller;

import com.collabHub.admin.dto.AdminStatisticsDTO;
import com.collabHub.admin.dto.WorkspaceStatisticsDTO;
import com.collabHub.admin.dto.SuspendWorkspaceRequestDTO;
import com.collabHub.admin.service.AdminService;
import com.collabHub.common.util.SecurityUtil;
import com.collabHub.user.dto.UserProfileDTO;
import com.collabHub.workspace.dto.WorkspaceResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;


    @GetMapping("/users")
    public ResponseEntity<Page<UserProfileDTO>> getAllUsers(
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("Admin requesting all users - page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        Page<UserProfileDTO> users = adminService.getAllUsers(pageable, currentUserEmail);
        
        return ResponseEntity.ok(users);
    }


    @GetMapping("/users/{id}")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long id) {
        log.info("Admin requesting user by ID: {}", id);
        
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        UserProfileDTO user = adminService.getUserById(id, currentUserEmail);
        
        return ResponseEntity.ok(user);
    }


    @GetMapping("/users/search")
    public ResponseEntity<Page<UserProfileDTO>> searchUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("Admin searching users - query: '{}', status: {}, role: {}", q, status, role);
        
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        Page<UserProfileDTO> results = adminService.searchUsers(q, status, role, pageable, currentUserEmail);
        
        return ResponseEntity.ok(results);
    }


    @PutMapping("/users/{id}/ban")
    public ResponseEntity<UserProfileDTO> banUser(@PathVariable Long id) {
        log.info("Admin banning user with ID: {}", id);
        
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        UserProfileDTO user = adminService.banUser(id, true, currentUserEmail);
        
        return ResponseEntity.ok(user);
    }


    @PutMapping("/users/{id}/unban")
    public ResponseEntity<UserProfileDTO> unbanUser(@PathVariable Long id) {
        log.info("Admin unbanning user with ID: {}", id);
        
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        UserProfileDTO user = adminService.banUser(id, false, currentUserEmail);
        
        return ResponseEntity.ok(user);
    }


    @GetMapping("/statistics")
    public ResponseEntity<AdminStatisticsDTO> getUserStatistics() {
        log.info("Admin requesting user statistics");
        
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        AdminStatisticsDTO statistics = adminService.getUserStatistics(currentUserEmail);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get all workspaces (Admin only)
     * Returns all workspaces in the system
     */
    @GetMapping("/workspaces")
    public ResponseEntity<List<WorkspaceResponseDTO>> getAllWorkspaces() {
        log.info("Admin requesting all workspaces");
        
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        List<WorkspaceResponseDTO> workspaces = adminService.getAllWorkspaces(currentUserEmail);
        
        return ResponseEntity.ok(workspaces);
    }


    @GetMapping("/workspaces/statistics")
    public ResponseEntity<WorkspaceStatisticsDTO> getWorkspaceStatistics() {
        log.info("Admin requesting workspace statistics");

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        WorkspaceStatisticsDTO stats =
                adminService.getWorkspaceStatistics(currentUserEmail);

        return ResponseEntity.ok(stats);
    }

    /**
     * Suspend a workspace (Admin only)
     * Prevents users from accessing or modifying the workspace
     * 
     * @param workspaceId the ID of the workspace to suspend
     * @param request suspension details (reason)
     * @return suspended workspace response
     */
    @PutMapping("/workspaces/{workspaceId}/suspend")
    public ResponseEntity<WorkspaceResponseDTO> suspendWorkspace(
            @PathVariable Long workspaceId,
            @Valid @RequestBody SuspendWorkspaceRequestDTO request) {
        log.info("Admin attempting to suspend workspace with ID: {}", workspaceId);
        
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        WorkspaceResponseDTO workspace = adminService.suspendWorkspace(workspaceId, request.getReason(), currentUserEmail);
        
        return ResponseEntity.ok(workspace);
    }

    /**
     * Unsuspend a workspace (Admin only)
     * Allows users to access the workspace again
     * 
     * @param workspaceId the ID of the workspace to unsuspend
     * @return unsuspended workspace response
     */
    @PutMapping("/workspaces/{workspaceId}/unsuspend")
    public ResponseEntity<WorkspaceResponseDTO> unsuspendWorkspace(@PathVariable Long workspaceId) {
        log.info("Admin attempting to unsuspend workspace with ID: {}", workspaceId);
        
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        WorkspaceResponseDTO workspace = adminService.unsuspendWorkspace(workspaceId, currentUserEmail);
        
        return ResponseEntity.ok(workspace);
    }
}
