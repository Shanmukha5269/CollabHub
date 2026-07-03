package com.collabHub.issue.service;

import com.collabHub.common.exception.*;
import com.collabHub.issue.dto.CreateIssueDTO;
import com.collabHub.issue.dto.IssueResponseDTO;
import com.collabHub.issue.dto.UpdateIssueDTO;
import com.collabHub.issue.dto.UpdateIssueStatusDTO;
import com.collabHub.issue.entity.Issue;
import com.collabHub.issue.entity.IssuePriority;
import com.collabHub.issue.entity.IssueStatus;
import com.collabHub.issue.entity.IssueType;
import com.collabHub.issue.repository.IssueRepository;
import com.collabHub.project.entity.Project;
import com.collabHub.project.repository.ProjectRepository;
import com.collabHub.user.entity.User;
import com.collabHub.user.entity.UserStatus;
import com.collabHub.user.repository.UserRepository;
import com.collabHub.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueServiceImpl implements IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public IssueResponseDTO createIssue(Long projectId, CreateIssueDTO dto, String reporterEmail) {
        log.info("Creating issue in project: {} by user: {}", projectId, reporterEmail);

        User reporter = findActiveUser(reporterEmail);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        if (project.getWorkspace().getSuspended()) {
            throw new WorkspaceSuspendedException("Cannot create issues in a suspended workspace");
        }

        requireWorkspaceMember(project.getWorkspace().getId(), reporter.getId());

        User assignee = resolveOptionalAssignee(dto.getAssigneeId(), project);

        int newCounter = project.getIssueCounter() + 1;
        project.setIssueCounter(newCounter);
        projectRepository.save(project);

        String issueKey = project.getProjectKey() + "-" + newCounter;

        long existingCount = issueRepository.countByProjectId(projectId);
        int position = (int) ((existingCount + 1) * 1000);

        Issue issue = Issue.builder()
                .issueKey(issueKey)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .type(dto.getType() != null ? dto.getType() : IssueType.TASK)
                .priority(dto.getPriority() != null ? dto.getPriority() : IssuePriority.MEDIUM)
                .status(IssueStatus.TODO)
                .project(project)
                .reporter(reporter)
                .assignee(assignee)
                .dueDate(dto.getDueDate())
                .position(position)
                .build();

        Issue saved = issueRepository.save(issue);
        log.info("Issue created: {} (ID: {})", saved.getIssueKey(), saved.getId());

        IssueResponseDTO responseDTO = convertToResponseDTO(saved);

        // Broadcast to all subscribers of this project's topic.
        //
        // Topic: /topic/projects/{projectId}
        // This is the same pattern as MessageServiceImpl which broadcasts to /topic/messages.
        // The difference is we scope it per-project so clients only receive
        // updates for projects they are subscribed to — not every project's updates.
        //
        // Payload includes an "event" field so the frontend knows
        // whether to add, update, or remove the issue from the board.
        broadcastIssueEvent(project.getId(), "ISSUE_CREATED", responseDTO);

        return responseDTO;
    }

    // -------------------------------------------------------------------------
    // Read  (no broadcast — reads don't change state)
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public IssueResponseDTO getIssueById(Long issueId, String userEmail) {
        log.info("Fetching issue ID: {} for user: {}", issueId, userEmail);

        User user = findActiveUser(userEmail);

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("Issue not found with ID: " + issueId));

        requireWorkspaceMember(issue.getProject().getWorkspace().getId(), user.getId());

        return convertToResponseDTO(issue);
    }

    @Override
    @Transactional(readOnly = true)
    public IssueResponseDTO getIssueByKey(String issueKey, String userEmail) {
        log.info("Fetching issue key: {} for user: {}", issueKey, userEmail);

        User user = findActiveUser(userEmail);

        Issue issue = issueRepository.findByIssueKey(issueKey.toUpperCase())
                .orElseThrow(() -> new IssueNotFoundException("Issue not found with key: " + issueKey));

        requireWorkspaceMember(issue.getProject().getWorkspace().getId(), user.getId());

        return convertToResponseDTO(issue);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IssueResponseDTO> getIssuesByProject(Long projectId, String userEmail, Pageable pageable) {
        User user = findActiveUser(userEmail);
        Project project = findAccessibleProject(projectId, user);
        return issueRepository.findByProjectIdPaginated(project.getId(), pageable)
                .map(this::convertToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IssueResponseDTO> getIssuesByProjectAndStatus(Long projectId, IssueStatus status, String userEmail, Pageable pageable) {
        User user = findActiveUser(userEmail);
        Project project = findAccessibleProject(projectId, user);
        return issueRepository.findByProjectIdAndStatus(project.getId(), status, pageable)
                .map(this::convertToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IssueResponseDTO> getIssuesByProjectAndPriority(Long projectId, IssuePriority priority, String userEmail, Pageable pageable) {
        User user = findActiveUser(userEmail);
        Project project = findAccessibleProject(projectId, user);
        return issueRepository.findByProjectIdAndPriority(project.getId(), priority, pageable)
                .map(this::convertToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IssueResponseDTO> getIssuesByProjectAndType(Long projectId, IssueType type, String userEmail, Pageable pageable) {
        User user = findActiveUser(userEmail);
        Project project = findAccessibleProject(projectId, user);
        return issueRepository.findByProjectIdAndType(project.getId(), type, pageable)
                .map(this::convertToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IssueResponseDTO> getIssuesByAssignee(Long projectId, Long assigneeId, String userEmail, Pageable pageable) {
        User user = findActiveUser(userEmail);
        Project project = findAccessibleProject(projectId, user);
        return issueRepository.findByProjectIdAndAssigneeId(project.getId(), assigneeId, pageable)
                .map(this::convertToResponseDTO);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public IssueResponseDTO updateIssue(Long issueId, UpdateIssueDTO dto, String userEmail) {
        log.info("Updating issue ID: {} by user: {}", issueId, userEmail);

        User user = findActiveUser(userEmail);

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("Issue not found with ID: " + issueId));

        if (issue.getProject().getWorkspace().getSuspended()) {
            throw new WorkspaceSuspendedException("Cannot update issues in a suspended workspace");
        }

        requireWorkspaceMember(issue.getProject().getWorkspace().getId(), user.getId());

        if (dto.getTitle() != null)       issue.setTitle(dto.getTitle());
        if (dto.getDescription() != null) issue.setDescription(dto.getDescription());
        if (dto.getPriority() != null)    issue.setPriority(dto.getPriority());
        if (dto.getType() != null)        issue.setType(dto.getType());
        if (dto.getDueDate() != null)     issue.setDueDate(dto.getDueDate());
        if (dto.getAssigneeId() != null) {
            User newAssignee = resolveOptionalAssignee(dto.getAssigneeId(), issue.getProject());
            issue.setAssignee(newAssignee);
        }

        Issue saved = issueRepository.save(issue);
        log.info("Issue updated: {}", saved.getIssueKey());

        IssueResponseDTO responseDTO = convertToResponseDTO(saved);

        // Broadcast update — same event pattern as ISSUE_CREATED
        broadcastIssueEvent(saved.getProject().getId(), "ISSUE_UPDATED", responseDTO);

        return responseDTO;
    }

    @Override
    @Transactional
    public IssueResponseDTO updateIssueStatus(Long issueId, UpdateIssueStatusDTO dto, String userEmail) {
        log.info("Updating status of issue ID: {} to {} by user: {}", issueId, dto.getStatus(), userEmail);

        User user = findActiveUser(userEmail);

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("Issue not found with ID: " + issueId));

        if (issue.getProject().getWorkspace().getSuspended()) {
            throw new WorkspaceSuspendedException("Cannot update issues in a suspended workspace");
        }

        requireWorkspaceMember(issue.getProject().getWorkspace().getId(), user.getId());

        issue.setStatus(dto.getStatus());

        Issue saved = issueRepository.save(issue);
        log.info("Issue {} status updated to: {}", saved.getIssueKey(), saved.getStatus());

        IssueResponseDTO responseDTO = convertToResponseDTO(saved);

        // Status change is the most frequent board event — broadcast immediately
        // so all connected clients see the card move columns in real time
        broadcastIssueEvent(saved.getProject().getId(), "ISSUE_STATUS_CHANGED", responseDTO);

        return responseDTO;
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteIssue(Long issueId, String userEmail) {
        log.info("Deleting issue ID: {} by user: {}", issueId, userEmail);

        User user = findActiveUser(userEmail);

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("Issue not found with ID: " + issueId));

        if (issue.getProject().getWorkspace().getSuspended()) {
            throw new WorkspaceSuspendedException("Cannot delete issues in a suspended workspace");
        }

        boolean isReporter = issue.getReporter().getId().equals(user.getId());
        boolean isWorkspaceOwner = issue.getProject().getWorkspace().getOwner().getId().equals(user.getId());

        if (!isReporter && !isWorkspaceOwner) {
            throw new UserAccessDeniedException("Only the issue reporter or workspace owner can delete this issue");
        }

        Long projectId = issue.getProject().getId();
        String issueKey = issue.getIssueKey();

        issueRepository.delete(issue);
        log.info("Issue deleted: {}", issueId);

        // For deletes we broadcast a minimal payload — just the key is enough
        // for the frontend to know which card to remove from the board
        broadcastDeleteEvent(projectId, issueKey);
    }

    // -------------------------------------------------------------------------
    // WebSocket broadcasting
    // -------------------------------------------------------------------------

    /**
     * Broadcast an issue create/update/status-change event to all subscribers
     * of /topic/projects/{projectId}.
     *
     * WHY per-project topics instead of one global /topic/issues?
     * MessageServiceImpl uses a single /topic/messages which means every client
     * receives every message from every channel — wasteful.
     * Per-project topics mean a client only receives events for projects
     * they are viewing, which is more efficient and scalable.
     *
     * The payload wraps the issue DTO inside an envelope with an "event" field
     * so the frontend knows what action to take:
     *   ISSUE_CREATED      → add card to board
     *   ISSUE_UPDATED      → update card in place
     *   ISSUE_STATUS_CHANGED → move card to different column
     *   ISSUE_DELETED      → remove card from board
     */
    private void broadcastIssueEvent(Long projectId, String event, IssueResponseDTO issue) {
        try {
            // Build a simple envelope: { "event": "ISSUE_CREATED", "data": { ...issue } }
            String payload = objectMapper.writeValueAsString(
                    new java.util.HashMap<>() {{
                        put("event", event);
                        put("data", issue);
                    }}
            );

            messagingTemplate.convertAndSend("/topic/projects/" + projectId, payload);

            log.info("Broadcasted {} for issue: {} to /topic/projects/{}",
                    event, issue.getIssueKey(), projectId);

        } catch (Exception e) {
            // log and continue, never let WS failure break the HTTP response
            log.error("Failed to broadcast issue event: {} for project: {}", event, projectId, e);
        }
    }

    /**
     * Broadcast a delete event with just the issue key.
     * The frontend only needs the key to find and remove the card from the board.
     */
    private void broadcastDeleteEvent(Long projectId, String issueKey) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new java.util.HashMap<>() {{
                        put("event", "ISSUE_DELETED");
                        put("issueKey", issueKey);
                    }}
            );

            messagingTemplate.convertAndSend("/topic/projects/" + projectId, payload);

            log.info("Broadcasted ISSUE_DELETED for issue: {} to /topic/projects/{}", issueKey, projectId);

        } catch (Exception e) {
            log.error("Failed to broadcast delete event for issue: {} project: {}", issueKey, projectId, e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private User findActiveUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }
        if (UserStatus.BANNED.equals(user.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }
        return user;
    }

    private void requireWorkspaceMember(Long workspaceId, Long userId) {
        boolean isMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(workspaceId, userId)
                .isPresent();
        if (!isMember) {
            throw new UserAccessDeniedException("You are not a member of this workspace");
        }
    }

    private Project findAccessibleProject(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));
        if (project.getWorkspace().getSuspended()) {
            throw new WorkspaceSuspendedException("This workspace is suspended");
        }
        requireWorkspaceMember(project.getWorkspace().getId(), user.getId());
        return project;
    }

    private User resolveOptionalAssignee(Long assigneeId, Project project) {
        if (assigneeId == null) return null;
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new UserNotFoundException("Assignee not found with ID: " + assigneeId));
        requireWorkspaceMember(project.getWorkspace().getId(), assignee.getId());
        return assignee;
    }

    private IssueResponseDTO convertToResponseDTO(Issue issue) {
        return IssueResponseDTO.builder()
                .id(issue.getId())
                .issueKey(issue.getIssueKey())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .status(issue.getStatus())
                .priority(issue.getPriority())
                .type(issue.getType())
                .projectId(issue.getProject().getId())
                .projectName(issue.getProject().getName())
                .projectKey(issue.getProject().getProjectKey())
                .reporter(IssueResponseDTO.UserMinimalDTO.builder()
                        .id(issue.getReporter().getId())
                        .name(issue.getReporter().getName())
                        .email(issue.getReporter().getEmail())
                        .build())
                .assignee(issue.getAssignee() != null
                        ? IssueResponseDTO.UserMinimalDTO.builder()
                        .id(issue.getAssignee().getId())
                        .name(issue.getAssignee().getName())
                        .email(issue.getAssignee().getEmail())
                        .build()
                        : null)
                .dueDate(issue.getDueDate())
                .position(issue.getPosition())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }
}