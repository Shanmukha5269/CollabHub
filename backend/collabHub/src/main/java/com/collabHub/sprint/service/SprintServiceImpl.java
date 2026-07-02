package com.collabHub.sprint.service;

import com.collabHub.common.exception.*;
import com.collabHub.issue.dto.IssueResponseDTO;
import com.collabHub.issue.entity.Issue;
import com.collabHub.issue.entity.IssueStatus;
import com.collabHub.issue.repository.IssueRepository;
import com.collabHub.project.entity.Project;
import com.collabHub.project.repository.ProjectRepository;
import com.collabHub.sprint.dto.BoardResponseDTO;
import com.collabHub.sprint.dto.CreateSprintDTO;
import com.collabHub.sprint.dto.SprintResponseDTO;
import com.collabHub.sprint.dto.UpdateSprintDTO;
import com.collabHub.sprint.entity.Sprint;
import com.collabHub.sprint.entity.SprintStatus;
import com.collabHub.sprint.repository.SprintRepository;
import com.collabHub.user.entity.User;
import com.collabHub.user.entity.UserStatus;
import com.collabHub.user.repository.UserRepository;
import com.collabHub.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SprintServiceImpl implements SprintService {

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SprintResponseDTO createSprint(Long projectId, CreateSprintDTO dto, String userEmail) {
        log.info("Creating sprint '{}' in project: {} by user: {}", dto.getName(), projectId, userEmail);

        User user = findActiveUser(userEmail);
        Project project = findAccessibleProject(projectId, user);

        Sprint sprint = Sprint.builder()
                .name(dto.getName())
                .goal(dto.getGoal())
                .project(project)
                .creator(user)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();

        Sprint saved = sprintRepository.save(sprint);
        log.info("Sprint created with ID: {}", saved.getId());

        return convertToResponseDTO(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public SprintResponseDTO getSprintById(Long sprintId, String userEmail) {
        log.info("Fetching sprint ID: {} for user: {}", sprintId, userEmail);

        User user = findActiveUser(userEmail);
        Sprint sprint = findSprint(sprintId);
        requireWorkspaceMember(sprint.getProject().getWorkspace().getId(), user.getId());

        return convertToResponseDTO(sprint);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SprintResponseDTO> getSprintsByProject(Long projectId, String userEmail) {
        log.info("Fetching sprints for project: {} for user: {}", projectId, userEmail);

        User user = findActiveUser(userEmail);
        findAccessibleProject(projectId, user);

        return sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SprintResponseDTO updateSprint(Long sprintId, UpdateSprintDTO dto, String userEmail) {
        log.info("Updating sprint ID: {} by user: {}", sprintId, userEmail);

        User user = findActiveUser(userEmail);
        Sprint sprint = findSprint(sprintId);
        requireWorkspaceMember(sprint.getProject().getWorkspace().getId(), user.getId());

        // Completed sprints cannot be edited
        if (SprintStatus.COMPLETED.equals(sprint.getStatus())) {
            throw new IllegalArgumentException("A completed sprint cannot be modified");
        }

        if (dto.getName() != null) sprint.setName(dto.getName());
        if (dto.getGoal() != null) sprint.setGoal(dto.getGoal());
        if (dto.getStartDate() != null) sprint.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) sprint.setEndDate(dto.getEndDate());

        Sprint saved = sprintRepository.save(sprint);
        log.info("Sprint updated: {}", sprintId);

        return convertToResponseDTO(saved);
    }

    // -------------------------------------------------------------------------
    // Start sprint  — PLANNING → ACTIVE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SprintResponseDTO startSprint(Long sprintId, String userEmail) {
        log.info("Starting sprint ID: {} by user: {}", sprintId, userEmail);

        User user = findActiveUser(userEmail);
        Sprint sprint = findSprint(sprintId);

        requireWorkspaceMember(sprint.getProject().getWorkspace().getId(), user.getId());

        // Guard 1: Sprint must be in PLANNING state
        if (!SprintStatus.PLANNING.equals(sprint.getStatus())) {
            throw new IllegalArgumentException(
                "Sprint cannot be started. Current status: " + sprint.getStatus() +
                ". Only PLANNING sprints can be started.");
        }

        // Guard 2: Only one ACTIVE sprint per project allowed.
        //
        // WHY this rule exists:
        // In Scrum, a sprint represents the team's focus for a fixed time period.
        // Having two active sprints simultaneously splits the team's attention
        // and makes it impossible to know which sprint the board should show.
        // Jira enforces this same rule.
        boolean alreadyHasActiveSprint = sprintRepository
                .existsByProjectIdAndStatus(sprint.getProject().getId(), SprintStatus.ACTIVE);

        if (alreadyHasActiveSprint) {
            throw new IllegalArgumentException(
                "Project already has an active sprint. Complete it before starting a new one.");
        }

        sprint.setStatus(SprintStatus.ACTIVE);
        sprint.setStartedAt(LocalDateTime.now());

        Sprint saved = sprintRepository.save(sprint);
        log.info("Sprint {} started at {}", sprintId, saved.getStartedAt());

        return convertToResponseDTO(saved);
    }

    // -------------------------------------------------------------------------
    // Complete sprint  — ACTIVE → COMPLETED
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SprintResponseDTO completeSprint(Long sprintId, String userEmail) {
        log.info("Completing sprint ID: {} by user: {}", sprintId, userEmail);

        User user = findActiveUser(userEmail);
        Sprint sprint = findSprint(sprintId);

        requireWorkspaceMember(sprint.getProject().getWorkspace().getId(), user.getId());

        // Guard: Sprint must be ACTIVE to be completed
        if (!SprintStatus.ACTIVE.equals(sprint.getStatus())) {
            throw new IllegalArgumentException(
                "Sprint cannot be completed. Current status: " + sprint.getStatus() +
                ". Only ACTIVE sprints can be completed.");
        }

        // Move unfinished issues back to the backlog.
        //
        // WHY we do this:
        // When a sprint ends, issues that weren't finished don't just disappear.
        // In Jira, they are moved to the backlog so the team can plan them
        // into the next sprint. We implement this by setting sprint = null,
        // which is our representation of "in the backlog".
        //
        // DONE issues stay linked to the sprint so history is preserved —
        // you can look at a completed sprint and see what was accomplished.
        List<Issue> sprintIssues = issueRepository.findBySprintId(sprintId);

        List<Issue> incompleteIssues = sprintIssues.stream()
                .filter(issue -> !IssueStatus.DONE.equals(issue.getStatus()))
                .collect(Collectors.toList());

        if (!incompleteIssues.isEmpty()) {
            log.info("Moving {} incomplete issues back to backlog for sprint: {}", incompleteIssues.size(), sprintId);
            incompleteIssues.forEach(issue -> issue.setSprint(null));
            issueRepository.saveAll(incompleteIssues);
        }

        sprint.setStatus(SprintStatus.COMPLETED);
        sprint.setCompletedAt(LocalDateTime.now());

        Sprint saved = sprintRepository.save(sprint);
        log.info("Sprint {} completed at {}. {} issues moved to backlog, {} completed.",
                sprintId, saved.getCompletedAt(), incompleteIssues.size(),
                sprintIssues.size() - incompleteIssues.size());

        return convertToResponseDTO(saved);
    }

    // -------------------------------------------------------------------------
    // Issue ↔ Sprint management
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SprintResponseDTO addIssueToSprint(Long sprintId, Long issueId, String userEmail) {
        log.info("Adding issue: {} to sprint: {} by user: {}", issueId, sprintId, userEmail);

        User user = findActiveUser(userEmail);
        Sprint sprint = findSprint(sprintId);

        requireWorkspaceMember(sprint.getProject().getWorkspace().getId(), user.getId());

        if (SprintStatus.COMPLETED.equals(sprint.getStatus())) {
            throw new IllegalArgumentException("Cannot add issues to a completed sprint");
        }

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("Issue not found with ID: " + issueId));

        // Issue must belong to the same project as the sprint
        if (!issue.getProject().getId().equals(sprint.getProject().getId())) {
            throw new IllegalArgumentException("Issue does not belong to this project");
        }

        issue.setSprint(sprint);
        issueRepository.save(issue);

        log.info("Issue {} added to sprint {}", issueId, sprintId);
        return convertToResponseDTO(sprint);
    }

    @Override
    @Transactional
    public SprintResponseDTO removeIssueFromSprint(Long sprintId, Long issueId, String userEmail) {
        log.info("Removing issue: {} from sprint: {} by user: {}", issueId, sprintId, userEmail);

        User user = findActiveUser(userEmail);
        Sprint sprint = findSprint(sprintId);

        requireWorkspaceMember(sprint.getProject().getWorkspace().getId(), user.getId());

        if (SprintStatus.COMPLETED.equals(sprint.getStatus())) {
            throw new IllegalArgumentException("Cannot remove issues from a completed sprint");
        }

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("Issue not found with ID: " + issueId));

        issue.setSprint(null);   // back to backlog
        issueRepository.save(issue);

        log.info("Issue {} removed from sprint {} (moved to backlog)", issueId, sprintId);
        return convertToResponseDTO(sprint);
    }

    // -------------------------------------------------------------------------
    // Board endpoint — issues grouped by status
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public BoardResponseDTO getBoardByProject(Long projectId, String userEmail) {
        log.info("Fetching board for project: {} for user: {}", projectId, userEmail);

        User user = findActiveUser(userEmail);
        Project project = findAccessibleProject(projectId, user);

        // Find the active sprint for this project — there can only be one.
        Optional<Sprint> activeSprint = sprintRepository
                .findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE);

        // WHY two different board modes:
        // When a sprint is active — the board shows sprint issues only.
        // The team is focused on finishing the sprint, not the whole backlog.
        //
        // When no sprint is active — the board shows backlog issues.
        // This lets the team see all work even between sprints.
        if (activeSprint.isPresent()) {
            return buildSprintBoard(project, activeSprint.get());
        } else {
            return buildBacklogBoard(project);
        }
    }

    /**
     * Build a board from the active sprint's issues, grouped by status.
     *
     * HOW it works:
     * Instead of fetching all issues and filtering in Java (slow, loads everything),
     * we fire 4 targeted SQL queries — one per status column — each with JOIN FETCH
     * to load reporter/assignee/project in the same query.
     * 4 efficient queries > 1 huge query > N+1 queries.
     */
    private BoardResponseDTO buildSprintBoard(Project project, Sprint sprint) {
        log.info("Building sprint board for sprint: {}", sprint.getId());

        List<IssueResponseDTO> todo       = toResponseDTOs(issueRepository.findBySprintIdAndStatus(sprint.getId(), IssueStatus.TODO));
        List<IssueResponseDTO> inProgress = toResponseDTOs(issueRepository.findBySprintIdAndStatus(sprint.getId(), IssueStatus.IN_PROGRESS));
        List<IssueResponseDTO> inReview   = toResponseDTOs(issueRepository.findBySprintIdAndStatus(sprint.getId(), IssueStatus.IN_REVIEW));
        List<IssueResponseDTO> done       = toResponseDTOs(issueRepository.findBySprintIdAndStatus(sprint.getId(), IssueStatus.DONE));

        int total = todo.size() + inProgress.size() + inReview.size() + done.size();

        return BoardResponseDTO.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .projectKey(project.getProjectKey())
                .activeSprint(convertToResponseDTO(sprint))
                .todo(todo)
                .inProgress(inProgress)
                .inReview(inReview)
                .done(done)
                .totalIssues(total)
                .build();
    }

    /**
     * Build a board from backlog issues (no active sprint).
     * Shows issues where sprint IS NULL, grouped by status.
     */
    private BoardResponseDTO buildBacklogBoard(Project project) {
        log.info("No active sprint — building backlog board for project: {}", project.getId());

        List<IssueResponseDTO> todo       = toResponseDTOs(issueRepository.findBacklogByProjectIdAndStatus(project.getId(), IssueStatus.TODO));
        List<IssueResponseDTO> inProgress = toResponseDTOs(issueRepository.findBacklogByProjectIdAndStatus(project.getId(), IssueStatus.IN_PROGRESS));
        List<IssueResponseDTO> inReview   = toResponseDTOs(issueRepository.findBacklogByProjectIdAndStatus(project.getId(), IssueStatus.IN_REVIEW));
        List<IssueResponseDTO> done       = toResponseDTOs(issueRepository.findBacklogByProjectIdAndStatus(project.getId(), IssueStatus.DONE));

        int total = todo.size() + inProgress.size() + inReview.size() + done.size();

        return BoardResponseDTO.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .projectKey(project.getProjectKey())
                .activeSprint(null)   // explicitly null — no active sprint
                .todo(todo)
                .inProgress(inProgress)
                .inReview(inReview)
                .done(done)
                .totalIssues(total)
                .build();
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteSprint(Long sprintId, String userEmail) {
        log.info("Deleting sprint ID: {} by user: {}", sprintId, userEmail);

        User user = findActiveUser(userEmail);
        Sprint sprint = findSprint(sprintId);

        requireWorkspaceMember(sprint.getProject().getWorkspace().getId(), user.getId());

        if (SprintStatus.ACTIVE.equals(sprint.getStatus())) {
            throw new IllegalArgumentException("An active sprint cannot be deleted. Complete it first.");
        }

        // Move any issues in this sprint back to the backlog before deleting
        List<Issue> sprintIssues = issueRepository.findBySprintId(sprintId);
        sprintIssues.forEach(issue -> issue.setSprint(null));
        issueRepository.saveAll(sprintIssues);

        sprintRepository.delete(sprint);
        log.info("Sprint deleted: {}", sprintId);
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

    private Sprint findSprint(Long sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException("Sprint not found with ID: " + sprintId));
    }

    /** Convert a list of Issue entities to IssueResponseDTOs. */
    private List<IssueResponseDTO> toResponseDTOs(List<Issue> issues) {
        return issues.stream()
                .map(this::convertIssueToResponseDTO)
                .collect(Collectors.toList());
    }

    private IssueResponseDTO convertIssueToResponseDTO(Issue issue) {
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

    private SprintResponseDTO convertToResponseDTO(Sprint sprint) {
        Long sprintId = sprint.getId();

        Long issueCount      = issueRepository.countBySprintId(sprintId);
        Long completedCount  = issueRepository.countBySprintIdAndStatus(sprintId, IssueStatus.DONE);

        return SprintResponseDTO.builder()
                .id(sprint.getId())
                .name(sprint.getName())
                .goal(sprint.getGoal())
                .status(sprint.getStatus())
                .projectId(sprint.getProject().getId())
                .projectName(sprint.getProject().getName())
                .projectKey(sprint.getProject().getProjectKey())
                .creatorId(sprint.getCreator().getId())
                .creatorName(sprint.getCreator().getName())
                .creatorEmail(sprint.getCreator().getEmail())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .startedAt(sprint.getStartedAt())
                .completedAt(sprint.getCompletedAt())
                .issueCount(issueCount.intValue())
                .completedIssueCount(completedCount.intValue())
                .createdAt(sprint.getCreatedAt())
                .updatedAt(sprint.getUpdatedAt())
                .build();
    }
}
