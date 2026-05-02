package com.collabHub.workspace.service;

import com.collabHub.common.exception.UserBannedException;
import com.collabHub.common.exception.UserNotFoundException;
import com.collabHub.common.exception.UserAccessDeniedException;
import com.collabHub.user.entity.Role;
import com.collabHub.user.entity.User;
import com.collabHub.user.entity.UserStatus;
import com.collabHub.user.repository.UserRepository;
import com.collabHub.workspace.dto.CreateWorkspaceRequestDTO;
import com.collabHub.workspace.dto.UserBasicInfoDTO;
import com.collabHub.workspace.dto.WorkspaceResponseDTO;
import com.collabHub.workspace.dto.WorkspaceMemberDTO;
import com.collabHub.workspace.entity.Workspace;
import com.collabHub.workspace.entity.WorkspaceMember;
import com.collabHub.workspace.entity.WorkspaceRole;
import com.collabHub.workspace.repository.WorkspaceRepository;
import com.collabHub.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository memberRepository;

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

        // 7. Add owner as member
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspace(savedWorkspace)
                .user(owner)
                .role(WorkspaceRole.OWNER)
                .canManageMembers(true)
                .build();
            
        memberRepository.save(ownerMember);
        log.info("Workspace created successfully with ID: {} for user: {}", savedWorkspace.getId(), ownerEmail);

        // 8. Convert to response DTO
        return convertToResponseDTO(savedWorkspace);
    }

    @Override
    public List<WorkspaceResponseDTO> getUserWorkspaces(String ownerEmail) {
        log.info("Fetching workspaces for user: {}", ownerEmail);

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

        // 4. If user is ADMIN, return all workspaces
        if (Role.ADMIN.equals(user.getRole())) {
            log.debug("Admin user {} requesting all workspaces", ownerEmail);
            List<Workspace> allWorkspaces = workspaceRepository.findAll();
            return allWorkspaces.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());
        }

        // 5. For regular users, return workspaces they are members of OR own
        List<WorkspaceMember> memberships = memberRepository.findByUserIdAndRemovedAtIsNull(user.getId());
        List<Workspace> memberWorkspaces = memberships.stream()
                .map(WorkspaceMember::getWorkspace)
                .collect(Collectors.toList());
        
        // 6. Also include workspaces where the user is the owner
        List<Workspace> ownedWorkspaces = workspaceRepository.findAll().stream()
                .filter(w -> w.getOwner().getId().equals(user.getId()))
                .collect(Collectors.toList());
        
        // 7. Combine and deduplicate: member workspaces + owned workspaces (in case owner is also a member)
        List<Workspace> allUserWorkspaces = new java.util.ArrayList<>(memberWorkspaces);
        ownedWorkspaces.forEach(owned -> {
            if (!allUserWorkspaces.stream().anyMatch(w -> w.getId().equals(owned.getId()))) {
                allUserWorkspaces.add(owned);
            }
        });
        
        log.info("Found {} workspaces for user: {}", allUserWorkspaces.size(), ownerEmail);

        // 8. Convert each workspace to response DTO and return as list
        return allUserWorkspaces.stream()
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
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> {
                    log.warn("Workspace {} not found", workspaceId);
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

        // 4. Find the workspace by ID
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> {
                    log.warn("Workspace {} not found", workspaceId);
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

        // 4. Find the workspace by ID
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> {
                    log.warn("Workspace {} not found", workspaceId);
                    return new IllegalArgumentException("Workspace not found");
                });

        // 5. Authorization check: Only owner or ADMIN can delete
        if (!isOwnerOrAdmin(user, workspace)) {
            log.warn("User {} attempted to delete workspace {} without proper permissions", ownerEmail, workspaceId);
            throw new IllegalArgumentException("You don't have permission to delete this workspace");
        }

        // 6. Delete all workspace members first (cascade delete)
        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspaceId);
        memberRepository.deleteAll(members);
        
        // 7. Perform hard delete (completely remove from database)
        workspaceRepository.deleteById(workspaceId);

        log.info("Workspace permanently deleted with ID: {} by user: {}", workspaceId, ownerEmail);
    }

    @Override
    public List<WorkspaceResponseDTO> getAllWorkspaces(String requesterEmail) {
        log.info("Admin requesting all workspaces");

        // 1. Find the user by email
        User user = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + requesterEmail));

        // 2. Check if user is soft-deleted
        if (user.getDeletedAt() != null) {
            log.warn("Deleted user {} attempted to fetch all workspaces", requesterEmail);
            throw new UserNotFoundException("User account is deleted");
        }

        // 3. Check if user is BANNED
        if (UserStatus.BANNED.equals(user.getStatus())) {
            log.warn("Banned user {} attempted to fetch all workspaces", requesterEmail);
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }

        // 4. Check if user is ADMIN
        if (!Role.ADMIN.equals(user.getRole())) {
            log.warn("Non-admin user {} attempted to fetch all workspaces", requesterEmail);
            throw new UserAccessDeniedException("Only admins can view all workspaces");
        }

        // 5. Fetch all workspaces
        List<Workspace> allWorkspaces = workspaceRepository.findAll();
        log.info("Returning {} workspaces to admin", allWorkspaces.size());

        // 6. Convert each workspace to response DTO and return as list
        return allWorkspaces.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
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

    private WorkspaceResponseDTO convertToResponseDTO(Workspace workspace) {
        // Get all active members of the workspace
        List<WorkspaceMember> members = memberRepository.findByWorkspaceIdAndRemovedAtIsNull(workspace.getId());
        List<WorkspaceMemberDTO> memberDTOs = members.stream()
                .map(member -> {
                    return WorkspaceMemberDTO.builder()
                            .id(member.getId())
                            .userId(member.getUser().getId())
                            .userName(member.getUser().getName())
                            .userEmail(member.getUser().getEmail())
                            .role(member.getRole())
                            .canManageMembers(member.getCanManageMembers())
                            .joinedAt(member.getJoinedAt())
                            .build();
                })
                .collect(Collectors.toList());
        
        return WorkspaceResponseDTO.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .owner(convertUserToBasicInfoDTO(workspace.getOwner()))
                .members(memberDTOs)
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
