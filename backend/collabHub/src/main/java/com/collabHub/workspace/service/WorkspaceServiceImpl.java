package com.collabHub.workspace.service;

import com.collabHub.common.exception.UserBannedException;
import com.collabHub.common.exception.UserNotFoundException;
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
