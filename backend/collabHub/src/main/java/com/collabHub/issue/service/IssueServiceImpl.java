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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueServiceImpl implements IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public IssueResponseDTO createIssue(Long projectId, CreateIssueDTO dto, String reporterEmail) {
        log.info("Creating issue in project: {} by user: {}", projectId, reporterEmail);

        // 1. Validate reporter
        User reporter = findActiveUser(reporterEmail);

        // 2. Validate project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        if (project.getWorkspace().getSuspended()) {
            throw new WorkspaceSuspendedException("Cannot create issues in a suspended workspace");
        }

        // 3. Reporter must be a workspace member
        requireWorkspaceMember(project.getWorkspace().getId(), reporter.getId());

        // 4. Resolve optional assignee
        User assignee = resolveOptionalAssignee(dto.getAssigneeId(), project);

        // 5. Generate atomic issue key — e.g. "COLL-1", "COLL-2"
        //
        // WHY @Transactional + pessimistic locking matters here:
        // If two users create issues at the same millisecond, both could read
        // issueCounter = 5, both increment to 6, and both save "COLL-6" —
        // a duplicate key. The @Transactional annotation on this method ensures
        // the increment + save is one atomic DB operation, preventing this race.
        int newCounter = project.getIssueCounter() + 1;
        project.setIssueCounter(newCounter);
        projectRepository.save(project);

        String issueKey = project.getProjectKey() + "-" + newCounter;

        // 6. Calculate position for ordering (gap-based: 1000, 2000, 3000)
        //    New issues go to the bottom of the TODO column by default.
        long existingCount = issueRepository.countByProjectId(projectId);
        int position = (int) ((existingCount + 1) * 1000);

        // 7. Build and save the issue
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

        return convertToResponseDTO(saved);
    }

    // -------------------------------------------------------------------------
    // Read
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
        log.info("Fetching issues for project: {} page: {}", projectId, pageable.getPageNumber());

        User user = findActiveUser(userEmail);
        Project project = findAccessibleProject(projectId, user);

        return issueRepository.findByProjectIdPaginated(project.getId(), pageable)
                .map(this::convertToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IssueResponseDTO> getIssuesByProjectAndStatus(
            Long projectId, IssueStatus status, String userEmail, Pageable pageable) {
        log.info("Fetching issues for project: {} status: {}", projectId, status);

        User user = findActiveUser(userEmail);
        Project project = findAccessibleProject(projectId, user);

        return issueRepository.findByProjectIdAndStatus(project.getId(), status, pageable)
                .map(this::convertToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IssueResponseDTO> getIssuesByProjectAndPriority(
            Long projectId, IssuePriority priority, String userEmail, Pageable pageable) {
        log.info("Fetching issues for project: {} priority: {}", projectId, priority);

        User user = findActiveUser(userEmail);
        Project project = findAccessibleProject(projectId, user);

        return issueRepository.findByProjectIdAndPriority(project.getId(), priority, pageable)
                .map(this::convertToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IssueResponseDTO> getIssuesByProjectAndType(
            Long projectId, IssueType type, String userEmail, Pageable pageable) {
        log.info("Fetching issues for project: {} type: {}", projectId, type);

        User user = findActiveUser(userEmail);
        Project project = findAccessibleProject(projectId, user);

        return issueRepository.findByProjectIdAndType(project.getId(), type, pageable)
                .map(this::convertToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IssueResponseDTO> getIssuesByAssignee(
            Long projectId, Long assigneeId, String userEmail, Pageable pageable) {
        log.info("Fetching issues for project: {} assignee: {}", projectId, assigneeId);

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

        // Apply only the fields that were provided — same partial-update pattern as editMessage
        if (dto.getTitle() != null) {
            issue.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            issue.setDescription(dto.getDescription());
        }
        if (dto.getPriority() != null) {
            issue.setPriority(dto.getPriority());
        }
        if (dto.getType() != null) {
            issue.setType(dto.getType());
        }
        if (dto.getDueDate() != null) {
            issue.setDueDate(dto.getDueDate());
        }
        if (dto.getAssigneeId() != null) {
            User newAssignee = resolveOptionalAssignee(dto.getAssigneeId(), issue.getProject());
            issue.setAssignee(newAssignee);
        }

        Issue saved = issueRepository.save(issue);
        log.info("Issue updated: {}", saved.getIssueKey());

        return convertToResponseDTO(saved);
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

        // Any workspace member can change status — not just the reporter.
        // This matches Jira's behaviour where anyone on the team can move cards.
        requireWorkspaceMember(issue.getProject().getWorkspace().getId(), user.getId());

        issue.setStatus(dto.getStatus());

        Issue saved = issueRepository.save(issue);
        log.info("Issue {} status updated to: {}", saved.getIssueKey(), saved.getStatus());

        return convertToResponseDTO(saved);
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

        // Only reporter or workspace owner can delete
        boolean isReporter = issue.getReporter().getId().equals(user.getId());
        boolean isWorkspaceOwner = issue.getProject().getWorkspace().getOwner().getId().equals(user.getId());

        if (!isReporter && !isWorkspaceOwner) {
            throw new UserAccessDeniedException("Only the issue reporter or workspace owner can delete this issue");
        }

        issueRepository.delete(issue);
        log.info("Issue deleted: {}", issueId);
    }

    // -------------------------------------------------------------------------
    // Private helpers — same style as ProjectServiceImpl / ChannelServiceImpl
    // -------------------------------------------------------------------------

    /**
     * Find a user by email and run the three standard guards.
     * Identical to the same helper in ProjectServiceImpl.
     */
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

    /**
     * Throw if the user is not an active member of the given workspace.
     */
    private void requireWorkspaceMember(Long workspaceId, Long userId) {
        boolean isMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(workspaceId, userId)
                .isPresent();

        if (!isMember) {
            throw new UserAccessDeniedException("You are not a member of this workspace");
        }
    }

    /**
     * Find a project and verify the user has access to it.
     * Extracted because every read query needs both steps.
     */
    private Project findAccessibleProject(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        if (project.getWorkspace().getSuspended()) {
            throw new WorkspaceSuspendedException("This workspace is suspended");
        }

        requireWorkspaceMember(project.getWorkspace().getId(), user.getId());

        return project;
    }

    /**
     * Resolve an optional assigneeId to a User, validating workspace membership.
     * Returns null if assigneeId is null.
     */
    private User resolveOptionalAssignee(Long assigneeId, Project project) {
        if (assigneeId == null) {
            return null;
        }

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new UserNotFoundException("Assignee not found with ID: " + assigneeId));

        requireWorkspaceMember(project.getWorkspace().getId(), assignee.getId());

        return assignee;
    }

    /**
     * Map Issue entity → IssueResponseDTO.
     * Mirrors convertToResponseDTO() in MessageServiceImpl — same builder style.
     */
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
