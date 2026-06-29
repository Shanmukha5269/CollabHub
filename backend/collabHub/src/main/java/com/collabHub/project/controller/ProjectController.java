package com.collabHub.project.controller;

import com.collabHub.common.util.SecurityUtil;
import com.collabHub.project.dto.CreateProjectDTO;
import com.collabHub.project.dto.ProjectResponseDTO;
import com.collabHub.project.dto.UpdateProjectDTO;
import com.collabHub.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Project Controller
 * Handles Jira-style project management endpoints.
 * Mirrors ChannelController structure exactly.
 */
@Slf4j
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Create a new project inside a workspace.
     * The authenticated user must be an active member of the workspace.
     *
     * @param request project creation details (name, key, workspaceId, optional leadId)
     * @return 201 Created with the new project
     */
    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(@Valid @RequestBody CreateProjectDTO request) {
        log.info("Project creation request: name='{}', key='{}', workspaceId={}",
                request.getName(), request.getProjectKey(), request.getWorkspaceId());

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        ProjectResponseDTO response = projectService.createProject(request, currentUserEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a project by its numeric ID.
     * The authenticated user must be an active workspace member.
     *
     * @param id project ID
     * @return 200 OK with project details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(@PathVariable Long id) {
        log.info("Get project request for ID: {}", id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        ProjectResponseDTO response = projectService.getProjectById(id, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Get a project by its key within a workspace (e.g. GET /api/projects/key/COLL?workspaceId=5).
     * Useful when the frontend knows the key but not the numeric ID.
     *
     * @param key         project key (e.g. COLL)
     * @param workspaceId workspace the project belongs to
     * @return 200 OK with project details
     */
    @GetMapping("/key/{key}")
    public ResponseEntity<ProjectResponseDTO> getProjectByKey(
            @PathVariable String key,
            @RequestParam Long workspaceId) {
        log.info("Get project by key='{}' in workspace={}", key, workspaceId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        ProjectResponseDTO response = projectService.getProjectByKey(key, workspaceId, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * List all projects in a specific workspace.
     * The authenticated user must be an active workspace member.
     *
     * @param workspaceId workspace ID
     * @return 200 OK with list of projects
     */
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<ProjectResponseDTO>> getProjectsByWorkspace(@PathVariable Long workspaceId) {
        log.info("Get projects request for workspace: {}", workspaceId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        List<ProjectResponseDTO> response = projectService.getProjectsByWorkspace(workspaceId, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * List all projects the authenticated user has access to, across all their workspaces.
     * Mirrors GET /api/channels for the Slack side.
     *
     * @return 200 OK with list of projects
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponseDTO>> getProjectsByUser() {
        log.info("Get all projects request for authenticated user");

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        List<ProjectResponseDTO> response = projectService.getProjectsByUser(currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Update a project's name, description, or lead.
     * Only the project creator or workspace owner may update.
     *
     * @param id      project ID
     * @param request fields to update (all optional — only provided fields are changed)
     * @return 200 OK with updated project
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectDTO request) {
        log.info("Update project request for ID: {}", id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        ProjectResponseDTO response = projectService.updateProject(id, request, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a project and all its issues.
     * Only the project creator or workspace owner may delete.
     *
     * @param id project ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        log.info("Delete project request for ID: {}", id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        projectService.deleteProject(id, currentUserEmail);

        return ResponseEntity.noContent().build();
    }
}
