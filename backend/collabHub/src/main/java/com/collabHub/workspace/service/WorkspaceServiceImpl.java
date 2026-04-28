package com.collabHub.workspace.service;

import com.collabHub.common.exception.UserBannedException;
import com.collabHub.common.exception.UserNotFoundException;
import com.collabHub.user.entity.Role;
import com.collabHub.user.entity.User;
import com.collabHub.user.entity.UserStatus;
import com.collabHub.user.repository.UserRepository;
import com.collabHub.workspace.dto.CreateWorkspaceRequestDTO;
import com.collabHub.workspace.dto.UserBasicInfoDTO;
import com.collabHub.workspace.dto.WorkspaceResponseDTO;
import com.collabHub.workspace.entity.Workspace;
import com.collabHub.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    @Override
    public WorkspaceResponseDTO createWorkspace(CreateWorkspaceRequestDTO request, String ownerEmail) {
        log.info("Creating workspace: '{}' for user: {}", request.getName(), ownerEmail);

        // 1. Find the owner user by email
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + ownerEmail));

        // 2. Check if user is soft-deleted (shouldn't be able to create workspaces)
        if (owner.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        // 3. Check if user is BANNED (blocked users cannot perform any action)
        if (UserStatus.BANNED.equals(owner.getStatus())) {
            log.warn("Banned user {} attempted to create workspace", ownerEmail);
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }

        // 4. Check if workspace with same name already exists for this user
        // (Workspace names must be unique per user, but can be same across different users)
        if (workspaceRepository.findByNameAndOwnerId(request.getName(), owner.getId()).isPresent()) {
            throw new IllegalArgumentException("Workspace with name '" + request.getName() + "' already exists");
        }

        // 5. Create new workspace
        Workspace workspace = Workspace.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .build();

        // 6. Save to database
        Workspace savedWorkspace = workspaceRepository.save(workspace);
        log.info("Workspace created successfully with ID: {} for user: {}", savedWorkspace.getId(), ownerEmail);

        // 7. Convert to response DTO
        return convertToResponseDTO(savedWorkspace);
    }

    @Override
    public List<WorkspaceResponseDTO> getUserWorkspaces(String ownerEmail) {
        log.info("Fetching all workspaces for user: {}", ownerEmail);

        // 1. Find the user by email
        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + ownerEmail));

        // 2. Check if user is soft-deleted
        if (user.getDeletedAt() != null) {
            log.warn("Deleted user {} attempted to fetch workspaces", ownerEmail);
            throw new UserNotFoundException("User account is deleted");
        }

        // 3. Check if user is BANNED
        if (UserStatus.BANNED.equals(user.getStatus())) {
            log.warn("Banned user {} attempted to fetch workspaces", ownerEmail);
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }

        // 4. Fetch ALL non-deleted workspaces (not restricted to owner)
        // This allows all authenticated users to see all workspaces
        List<Workspace> workspaces = workspaceRepository.findAllByDeletedAtIsNull();
        log.info("Found {} workspaces for user: {}", workspaces.size(), ownerEmail);

        // 5. Convert each workspace to response DTO and return as list
        return workspaces.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public WorkspaceResponseDTO getWorkspaceById(Long workspaceId, String ownerEmail) {
        log.info("Fetching workspace ID: {} for user: {}", workspaceId, ownerEmail);

        // 1. Find the user by email
        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + ownerEmail));

        // 2. Check if user is soft-deleted
        if (user.getDeletedAt() != null) {
            log.warn("Deleted user {} attempted to access workspace", ownerEmail);
            throw new UserNotFoundException("User account is deleted");
        }

        // 3. Check if user is BANNED
        if (UserStatus.BANNED.equals(user.getStatus())) {
            log.warn("Banned user {} attempted to access workspace", ownerEmail);
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }

        // 4. Find the workspace by ID (not restricted by ownership)
        // Any authenticated user can view any workspace
        Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> {
                    log.warn("Workspace {} not found or is deleted", workspaceId);
                    return new IllegalArgumentException("Workspace not found");
                });

        log.info("Workspace found: {}", workspaceId);

        // 5. Convert to response DTO
        return convertToResponseDTO(workspace);
    }

    @Override
    public WorkspaceResponseDTO updateWorkspace(Long workspaceId, CreateWorkspaceRequestDTO request, String ownerEmail) {
        log.info("Updating workspace ID: {} for user: {}", workspaceId, ownerEmail);

        // 1. Find the user by email
        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + ownerEmail));

        // 2. Check if user is soft-deleted
        if (user.getDeletedAt() != null) {
            log.warn("Deleted user {} attempted to update workspace", ownerEmail);
            throw new UserNotFoundException("User account is deleted");
        }

        // 3. Check if user is BANNED
        if (UserStatus.BANNED.equals(user.getStatus())) {
            log.warn("Banned user {} attempted to update workspace", ownerEmail);
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }

        // 4. Find the workspace by ID (and it's not deleted)
        Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> {
                    log.warn("Workspace {} not found or is deleted", workspaceId);
                    return new IllegalArgumentException("Workspace not found");
                });

        // 5. Authorization check: Only owner or ADMIN can update
        if (!isOwnerOrAdmin(user, workspace)) {
            log.warn("User {} attempted to update workspace {} without proper permissions", ownerEmail, workspaceId);
            throw new IllegalArgumentException("You don't have permission to update this workspace");
        }

        // 6. Check if new workspace name already exists (and is different from current name)
        // Name must be unique per owner
        if (!workspace.getName().equals(request.getName()) &&
                workspaceRepository.findByNameAndOwnerId(request.getName(), workspace.getOwner().getId()).isPresent()) {
            throw new IllegalArgumentException("Workspace with name '" + request.getName() + "' already exists");
        }

        // 7. Update workspace fields
        workspace.setName(request.getName());
        workspace.setDescription(request.getDescription());

        // 8. Save updated workspace
        Workspace updatedWorkspace = workspaceRepository.save(workspace);
        log.info("Workspace updated successfully with ID: {} by user: {}", workspaceId, ownerEmail);

        // 9. Convert to response DTO
        return convertToResponseDTO(updatedWorkspace);
    }

    @Override
    public void deleteWorkspace(Long workspaceId, String ownerEmail) {
        log.info("Deleting workspace ID: {} for user: {}", workspaceId, ownerEmail);

        // 1. Find the user by email
        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + ownerEmail));

        // 2. Check if user is soft-deleted
        if (user.getDeletedAt() != null) {
            log.warn("Deleted user {} attempted to delete workspace", ownerEmail);
            throw new UserNotFoundException("User account is deleted");
        }

        // 3. Check if user is BANNED
        if (UserStatus.BANNED.equals(user.getStatus())) {
            log.warn("Banned user {} attempted to delete workspace", ownerEmail);
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }

        // 4. Find the workspace by ID (and it's not already deleted)
        Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> {
                    log.warn("Workspace {} not found or is already deleted", workspaceId);
                    return new IllegalArgumentException("Workspace not found");
                });

        // 5. Authorization check: Only owner or ADMIN can delete
        if (!isOwnerOrAdmin(user, workspace)) {
            log.warn("User {} attempted to delete workspace {} without proper permissions", ownerEmail, workspaceId);
            throw new IllegalArgumentException("You don't have permission to delete this workspace");
        }

        // 6. Perform soft delete (set deletedAt timestamp)
        workspace.setDeletedAt(LocalDateTime.now());
        workspaceRepository.save(workspace);

        log.info("Workspace soft-deleted successfully with ID: {} by user: {}", workspaceId, ownerEmail);
    }

    /**
     * Helper method to check if user is authorized to manage workspace
     * User is authorized if they are:
     * 1. The workspace owner, OR
     * 2. An ADMIN user
     * 
     * @param user the user attempting the action
     * @param workspace the workspace being managed
     * @return true if user is owner or admin, false otherwise
     */
    private boolean isOwnerOrAdmin(User user, Workspace workspace) {
        // ADMIN role has global permissions
        if (Role.ADMIN.equals(user.getRole())) {
            log.debug("User {} has ADMIN role - granting permission", user.getEmail());
            return true;
        }
        // Non-admin users must be the workspace owner
        boolean isOwner = workspace.getOwner().getId().equals(user.getId());
        if (isOwner) {
            log.debug("User {} is workspace owner - granting permission", user.getEmail());
        }
        return isOwner;
    }

    /**
     * Helper method to convert Workspace entity to WorkspaceResponseDTO
     */
    private WorkspaceResponseDTO convertToResponseDTO(Workspace workspace) {
        return WorkspaceResponseDTO.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .owner(convertUserToBasicInfoDTO(workspace.getOwner()))
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }

    /**
     * Helper method to convert User entity to UserBasicInfoDTO
     * Only includes essential fields - no null values
     */
    private UserBasicInfoDTO convertUserToBasicInfoDTO(User user) {
        return UserBasicInfoDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
