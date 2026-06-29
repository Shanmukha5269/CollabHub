package com.collabHub.project.service;

import com.collabHub.common.exception.ProjectNotFoundException;
import com.collabHub.common.exception.UserAccessDeniedException;
import com.collabHub.common.exception.UserBannedException;
import com.collabHub.common.exception.UserNotFoundException;
import com.collabHub.project.dto.CreateProjectDTO;
import com.collabHub.project.dto.ProjectResponseDTO;
import com.collabHub.project.dto.UpdateProjectDTO;
import com.collabHub.project.entity.Project;
import com.collabHub.project.repository.ProjectRepository;
import com.collabHub.user.entity.User;
import com.collabHub.user.entity.UserStatus;
import com.collabHub.user.repository.UserRepository;
import com.collabHub.workspace.entity.Workspace;
import com.collabHub.workspace.repository.WorkspaceMemberRepository;
import com.collabHub.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    // Create

    @Override
    @Transactional
    public ProjectResponseDTO createProject(CreateProjectDTO dto, String creatorEmail) {
        log.info("Creating project '{}' (key={}) in workspace {} for user: {}",
                dto.getName(), dto.getProjectKey(), dto.getWorkspaceId(), creatorEmail);

        // 1. Validate creator — same three-guard block used everywhere in the codebase
        User creator = findActiveUser(creatorEmail);

        // 2. Validate workspace
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found with ID: " + dto.getWorkspaceId()));

        if (workspace.getSuspended()) {
            throw new IllegalArgumentException("Cannot create projects in a suspended workspace");
        }

        // 3. Workspace membership check — same pattern as ChannelServiceImpl
        requireWorkspaceMember(workspace.getId(), creator.getId());

        // 4. Uniqueness guards within the workspace
        if (projectRepository.existsByProjectKeyAndWorkspace(dto.getProjectKey(), workspace)) {
            throw new IllegalArgumentException(
                    "Project key '" + dto.getProjectKey() + "' is already used in this workspace");
        }
        if (projectRepository.existsByNameAndWorkspace(dto.getName(), workspace)) {
            throw new IllegalArgumentException(
                    "A project named '" + dto.getName() + "' already exists in this workspace");
        }

        // 5. Resolve optional lead
        User lead = resolveOptionalLead(dto.getLeadId(), workspace);

        // 6. Persist
        Project project = Project.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .projectKey(dto.getProjectKey().toUpperCase())
                .workspace(workspace)
                .creator(creator)
                .lead(lead)
                .build();

        Project saved = projectRepository.save(project);
        log.info("Project created successfully with ID: {}", saved.getId());

        return convertToResponseDTO(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ProjectResponseDTO getProjectById(Long projectId, String userEmail) {
        log.info("Fetching project ID: {} for user: {}", projectId, userEmail);

        User user = findActiveUser(userEmail);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        checkProjectAccess(project, user);

        return convertToResponseDTO(project);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponseDTO getProjectByKey(String projectKey, Long workspaceId, String userEmail) {
        log.info("Fetching project key={} in workspace={} for user: {}", projectKey, workspaceId, userEmail);

        User user = findActiveUser(userEmail);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found with ID: " + workspaceId));

        Project project = projectRepository.findByProjectKeyAndWorkspace(projectKey.toUpperCase(), workspace)
                .orElseThrow(() -> new ProjectNotFoundException(
                        "Project with key '" + projectKey + "' not found in workspace " + workspaceId));

        checkProjectAccess(project, user);

        return convertToResponseDTO(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getProjectsByWorkspace(Long workspaceId, String userEmail) {
        log.info("Fetching projects for workspace: {} for user: {}", workspaceId, userEmail);

        User user = findActiveUser(userEmail);

        requireWorkspaceMember(workspaceId, user.getId());

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found with ID: " + workspaceId));

        if (workspace.getSuspended()) {
            throw new IllegalArgumentException("This workspace is suspended. Projects cannot be accessed.");
        }

        List<Project> projects = projectRepository.findByWorkspace(workspace);
        log.info("Retrieved {} projects for workspace: {}", projects.size(), workspaceId);

        return projects.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getProjectsByUser(String userEmail) {
        log.info("Fetching all projects for user: {}", userEmail);

        User user = findActiveUser(userEmail);

        // Collect all active workspace IDs the user belongs to
        // Mirrors ChannelServiceImpl.getChannelsByUser() exactly
        List<Long> workspaceIds = workspaceMemberRepository
                .findByUserIdAndRemovedAtIsNull(user.getId())
                .stream()
                .map(member -> member.getWorkspace())
                .filter(ws -> !ws.getSuspended())
                .map(ws -> ws.getId())
                .collect(Collectors.toList());

        if (workspaceIds.isEmpty()) {
            log.info("User {} is not a member of any active workspace", userEmail);
            return List.of();
        }

        List<Project> projects = projectRepository.findByWorkspaceIdIn(workspaceIds);
        log.info("Retrieved {} projects for user: {} across {} workspaces",
                projects.size(), userEmail, workspaceIds.size());

        return projects.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ProjectResponseDTO updateProject(Long projectId, UpdateProjectDTO dto, String userEmail) {
        log.info("Updating project ID: {} for user: {}", projectId, userEmail);

        User user = findActiveUser(userEmail);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        if (project.getWorkspace().getSuspended()) {
            throw new IllegalArgumentException("Cannot update projects in a suspended workspace");
        }

        // Only creator or workspace owner may update — same rule as ChannelServiceImpl
        if (!isProjectOwner(project, user)) {
            throw new UserAccessDeniedException("Only the project creator or workspace owner can update this project");
        }

        // Apply changes
        if (dto.getName() != null) {
            if (!project.getName().equals(dto.getName()) &&
                    projectRepository.existsByNameAndWorkspace(dto.getName(), project.getWorkspace())) {
                throw new IllegalArgumentException(
                        "A project named '" + dto.getName() + "' already exists in this workspace");
            }
            project.setName(dto.getName());
        }

        if (dto.getDescription() != null) {
            project.setDescription(dto.getDescription());
        }

        if (dto.getLeadId() != null) {
            User newLead = userRepository.findById(dto.getLeadId())
                    .orElseThrow(() -> new UserNotFoundException("Lead user not found with ID: " + dto.getLeadId()));
            requireWorkspaceMember(project.getWorkspace().getId(), newLead.getId());
            project.setLead(newLead);
        }

        Project saved = projectRepository.save(project);
        log.info("Project updated successfully with ID: {}", projectId);

        return convertToResponseDTO(saved);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteProject(Long projectId, String userEmail) {
        log.info("Deleting project ID: {} for user: {}", projectId, userEmail);

        User user = findActiveUser(userEmail);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        if (project.getWorkspace().getSuspended()) {
            throw new IllegalArgumentException("Cannot delete projects in a suspended workspace");
        }

        if (!isProjectOwner(project, user)) {
            throw new UserAccessDeniedException("Only the project creator or workspace owner can delete this project");
        }

        projectRepository.delete(project);
        log.info("Project deleted successfully with ID: {}", projectId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Find a user by email and enforce the three standard guards:
     * account exists, not deleted, not banned.
     * Every service method in the codebase starts with this block.
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
     * Mirrors the inline check used throughout ChannelServiceImpl.
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
     * Verify the requesting user can see this project.
     * Projects are visible to all active workspace members (no private/public distinction yet).
     */
    private void checkProjectAccess(Project project, User user) {
        if (project.getWorkspace().getSuspended()) {
            throw new IllegalArgumentException("This workspace is suspended and projects cannot be accessed");
        }

        requireWorkspaceMember(project.getWorkspace().getId(), user.getId());
    }

    /**
     * A user is the project owner if they are the creator OR the workspace owner.
     */
    private boolean isProjectOwner(Project project, User user) {
        if (project.getCreator().getId().equals(user.getId())) {
            return true;
        }
        return project.getWorkspace().getOwner().getId().equals(user.getId());
    }

    /**
     * Resolve an optional leadId to a User, validating workspace membership.
     * Returns null if leadId is null (no lead assigned).
     */
    private User resolveOptionalLead(Long leadId, Workspace workspace) {
        if (leadId == null) {
            return null;
        }
        User lead = userRepository.findById(leadId)
                .orElseThrow(() -> new UserNotFoundException("Lead user not found with ID: " + leadId));
        requireWorkspaceMember(workspace.getId(), lead.getId());
        return lead;
    }

    /**
     * Map entity → DTO.
     * Mirrors convertToResponseDTO() in ChannelServiceImpl — same field ordering style.
     */
    private ProjectResponseDTO convertToResponseDTO(Project project) {
        return ProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .projectKey(project.getProjectKey())
                .workspaceId(project.getWorkspace().getId())
                .workspaceName(project.getWorkspace().getName())
                .creatorId(project.getCreator().getId())
                .creatorName(project.getCreator().getName())
                .creatorEmail(project.getCreator().getEmail())
                .leadId(project.getLead() != null ? project.getLead().getId() : null)
                .leadName(project.getLead() != null ? project.getLead().getName() : null)
                .leadEmail(project.getLead() != null ? project.getLead().getEmail() : null)
                .workspaceOwnerId(project.getWorkspace().getOwner().getId())
                .workspaceOwnerName(project.getWorkspace().getOwner().getName())
                .workspaceOwnerEmail(project.getWorkspace().getOwner().getEmail())
                .issueCount(project.getIssueCounter())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
