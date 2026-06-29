package com.collabHub.issue.controller;

import com.collabHub.common.util.SecurityUtil;
import com.collabHub.issue.dto.CreateIssueDTO;
import com.collabHub.issue.dto.IssueResponseDTO;
import com.collabHub.issue.dto.UpdateIssueDTO;
import com.collabHub.issue.dto.UpdateIssueStatusDTO;
import com.collabHub.issue.entity.IssuePriority;
import com.collabHub.issue.entity.IssueStatus;
import com.collabHub.issue.entity.IssueType;
import com.collabHub.issue.service.IssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Issue Controller
 * Handles Jira-style issue management endpoints.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Create a new issue inside a project.
     *
     * @param projectId project the issue belongs to
     * @param request   issue details (title required; type, priority, assignee optional)
     * @return 201 Created with the new issue
     */
    @PostMapping("/api/projects/{projectId}/issues")
    public ResponseEntity<IssueResponseDTO> createIssue(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateIssueDTO request) {
        log.info("Create issue request for project: {}", projectId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        IssueResponseDTO response = issueService.createIssue(projectId, request, currentUserEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a single issue by its numeric ID.
     *
     * @param projectId project ID (used for URL consistency)
     * @param id        issue ID
     * @return 200 OK with issue details
     */
    @GetMapping("/api/projects/{projectId}/issues/{id}")
    public ResponseEntity<IssueResponseDTO> getIssueById(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        log.info("Get issue request for ID: {} in project: {}", id, projectId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        IssueResponseDTO response = issueService.getIssueById(id, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Get an issue by its human-readable key (e.g. "COLL-1").
     * Standalone endpoint — not nested under project since the key is globally unique.
     *
     * @param key issue key
     * @return 200 OK with issue details
     */
    @GetMapping("/api/issues/key/{key}")
    public ResponseEntity<IssueResponseDTO> getIssueByKey(@PathVariable String key) {
        log.info("Get issue by key: {}", key);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        IssueResponseDTO response = issueService.getIssueByKey(key, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * List all issues for a project — paginated.
     * Supports optional filtering by status, priority, type, or assignee.
     *
     * Mirrors MessageController.getChannelMessages() exactly:
     * same page/size validation, same descending createdAt sort default.
     *
     * Examples:
     *   GET /api/projects/5/issues
     *   GET /api/projects/5/issues?status=IN_PROGRESS
     *   GET /api/projects/5/issues?priority=HIGH&page=0&size=10
     *   GET /api/projects/5/issues?assigneeId=3
     *
     * @param projectId  project ID
     * @param status     optional filter by IssueStatus
     * @param priority   optional filter by IssuePriority
     * @param type       optional filter by IssueType
     * @param assigneeId optional filter by assignee user ID
     * @param page       page number (0-indexed)
     * @param size       page size (max 100)
     * @return paginated page of issues
     */
    @GetMapping("/api/projects/{projectId}/issues")
    public ResponseEntity<Page<IssueResponseDTO>> getIssuesByProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) IssuePriority priority,
            @RequestParam(required = false) IssueType type,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Get issues request for project: {} — status={}, priority={}, type={}, assigneeId={}, page={}, size={}",
                projectId, status, priority, type, assigneeId, page, size);

        // Validate and cap page size — mirrors MessageController exactly
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;
        if (size < 1) size = DEFAULT_PAGE_SIZE;

        Pageable pageable = PageRequest.of(page, size, Sort.by("position").ascending());
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();

        Page<IssueResponseDTO> response;

        // Route to the correct filtered query based on which filter was provided.
        // Only one filter is applied at a time — multiple filter support is Phase 3+.
        if (status != null) {
            response = issueService.getIssuesByProjectAndStatus(projectId, status, currentUserEmail, pageable);
        } else if (priority != null) {
            response = issueService.getIssuesByProjectAndPriority(projectId, priority, currentUserEmail, pageable);
        } else if (type != null) {
            response = issueService.getIssuesByProjectAndType(projectId, type, currentUserEmail, pageable);
        } else if (assigneeId != null) {
            response = issueService.getIssuesByAssignee(projectId, assigneeId, currentUserEmail, pageable);
        } else {
            response = issueService.getIssuesByProject(projectId, currentUserEmail, pageable);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Update an issue's title, description, priority, type, assignee, or due date.
     * All fields in UpdateIssueDTO are optional — only provided fields are changed.
     *
     * @param projectId project ID
     * @param id        issue ID
     * @param request   fields to update
     * @return 200 OK with updated issue
     */
    @PutMapping("/api/projects/{projectId}/issues/{id}")
    public ResponseEntity<IssueResponseDTO> updateIssue(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateIssueDTO request) {
        log.info("Update issue request for ID: {} in project: {}", id, projectId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        IssueResponseDTO response = issueService.updateIssue(id, request, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Update only the status of an issue.
     * Dedicated PATCH endpoint — used by the board's drag-and-drop and
     * the status dropdown in the issue detail view.
     *
     * @param projectId project ID
     * @param id        issue ID
     * @param request   new status
     * @return 200 OK with updated issue
     */
    @PatchMapping("/api/projects/{projectId}/issues/{id}/status")
    public ResponseEntity<IssueResponseDTO> updateIssueStatus(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateIssueStatusDTO request) {
        log.info("Update status request for issue ID: {} to: {}", id, request.getStatus());

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        IssueResponseDTO response = issueService.updateIssueStatus(id, request, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete an issue.
     * Only the reporter or workspace owner can delete.
     *
     * @param projectId project ID
     * @param id        issue ID
     * @return 204 No Content
     */
    @DeleteMapping("/api/projects/{projectId}/issues/{id}")
    public ResponseEntity<Void> deleteIssue(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        log.info("Delete issue request for ID: {} in project: {}", id, projectId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        issueService.deleteIssue(id, currentUserEmail);

        return ResponseEntity.noContent().build();
    }
}
