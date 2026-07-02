package com.collabHub.sprint.controller;

import com.collabHub.common.util.SecurityUtil;
import com.collabHub.sprint.dto.BoardResponseDTO;
import com.collabHub.sprint.dto.CreateSprintDTO;
import com.collabHub.sprint.dto.SprintResponseDTO;
import com.collabHub.sprint.dto.UpdateSprintDTO;
import com.collabHub.sprint.service.SprintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Sprint Controller + Board Controller combined.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    // -------------------------------------------------------------------------
    // Sprint CRUD
    // -------------------------------------------------------------------------

    @PostMapping("/api/projects/{projectId}/sprints")
    public ResponseEntity<SprintResponseDTO> createSprint(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateSprintDTO request) {
        log.info("Create sprint request for project: {}", projectId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        SprintResponseDTO response = sprintService.createSprint(projectId, request, currentUserEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/projects/{projectId}/sprints")
    public ResponseEntity<List<SprintResponseDTO>> getSprintsByProject(@PathVariable Long projectId) {
        log.info("Get sprints request for project: {}", projectId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        List<SprintResponseDTO> response = sprintService.getSprintsByProject(projectId, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/projects/{projectId}/sprints/{id}")
    public ResponseEntity<SprintResponseDTO> getSprintById(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        log.info("Get sprint request for ID: {} in project: {}", id, projectId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        SprintResponseDTO response = sprintService.getSprintById(id, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/projects/{projectId}/sprints/{id}")
    public ResponseEntity<SprintResponseDTO> updateSprint(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateSprintDTO request) {
        log.info("Update sprint request for ID: {} in project: {}", id, projectId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        SprintResponseDTO response = sprintService.updateSprint(id, request, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/projects/{projectId}/sprints/{id}")
    public ResponseEntity<Void> deleteSprint(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        log.info("Delete sprint request for ID: {} in project: {}", id, projectId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        sprintService.deleteSprint(id, currentUserEmail);

        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Sprint lifecycle actions
    // -------------------------------------------------------------------------

    /**
     * Start a sprint — transitions it from PLANNING → ACTIVE.
     * Will fail if the project already has an active sprint.
     *
     * Uses POST (not PATCH) because this is a deliberate action/event,
     * not just updating a field. Same reason Jira uses POST for transitions.
     */
    @PatchMapping("/api/projects/{projectId}/sprints/{id}/start")
    public ResponseEntity<SprintResponseDTO> startSprint(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        log.info("Start sprint request for ID: {} in project: {}", id, projectId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        SprintResponseDTO response = sprintService.startSprint(id, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Complete a sprint — transitions it from ACTIVE → COMPLETED.
     * Incomplete issues are automatically moved back to the backlog.
     */
    @PatchMapping("/api/projects/{projectId}/sprints/{id}/complete")
    public ResponseEntity<SprintResponseDTO> completeSprint(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        log.info("Complete sprint request for ID: {} in project: {}", id, projectId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        SprintResponseDTO response = sprintService.completeSprint(id, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Issue ↔ Sprint management
    // -------------------------------------------------------------------------

    /**
     * Add an issue to a sprint (move it out of the backlog into the sprint).
     */
    @PostMapping("/api/projects/{projectId}/sprints/{id}/issues/{issueId}")
    public ResponseEntity<SprintResponseDTO> addIssueToSprint(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @PathVariable Long issueId) {
        log.info("Add issue: {} to sprint: {} request", issueId, id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        SprintResponseDTO response = sprintService.addIssueToSprint(id, issueId, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Remove an issue from a sprint (moves it back to the backlog).
     */
    @DeleteMapping("/api/projects/{projectId}/sprints/{id}/issues/{issueId}")
    public ResponseEntity<SprintResponseDTO> removeIssueFromSprint(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @PathVariable Long issueId) {
        log.info("Remove issue: {} from sprint: {} request", issueId, id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        SprintResponseDTO response = sprintService.removeIssueFromSprint(id, issueId, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Board
    // -------------------------------------------------------------------------

    /**
     * Get the Kanban board for a project.
     *
     * Returns issues grouped by status column (TODO, IN_PROGRESS, IN_REVIEW, DONE).
     * If a sprint is active → shows that sprint's issues.
     * If no sprint is active → shows backlog issues.
     *
     * This is a pure GET — no state is changed, just data is read and grouped.
     */
    @GetMapping("/api/projects/{projectId}/board")
    public ResponseEntity<BoardResponseDTO> getBoard(@PathVariable Long projectId) {
        log.info("Get board request for project: {}", projectId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        BoardResponseDTO response = sprintService.getBoardByProject(projectId, currentUserEmail);

        return ResponseEntity.ok(response);
    }
}
