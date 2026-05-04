package com.collabHub.admin.service;

import com.collabHub.admin.dto.AdminStatisticsDTO;
import com.collabHub.admin.dto.WorkspaceStatisticsDTO;
import com.collabHub.common.exception.UserAccessDeniedException;
import com.collabHub.common.exception.UserNotFoundException;
import com.collabHub.user.dto.UserProfileDTO;
import com.collabHub.user.entity.Role;
import com.collabHub.user.entity.User;
import com.collabHub.user.entity.UserStatus;
import com.collabHub.user.repository.UserRepository;
import com.collabHub.workspace.dto.WorkspaceResponseDTO;
import com.collabHub.workspace.entity.Workspace;
import com.collabHub.workspace.repository.WorkspaceMemberRepository;
import com.collabHub.workspace.repository.WorkspaceRepository;
import com.collabHub.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    /**
     * Verify that the current user is an ADMIN
     * Throws exception if user is not ADMIN
     */
    private User verifyAdminRole(String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        if (!currentUser.getRole().equals(Role.ADMIN)) {
            log.warn("Non-admin user {} attempted admin operation", currentUserEmail);
            throw new UserAccessDeniedException("Only administrators can perform this operation");
        }

        return currentUser;
    }

    @Override
    public Page<UserProfileDTO> getAllUsers(Pageable pageable, String currentUserEmail) {
        log.info("Admin {} requesting all users", currentUserEmail);
        
        // Verify admin role
        verifyAdminRole(currentUserEmail);

        // Get all active (non-deleted) users with pagination
        Page<User> users = userRepository.findAllActiveUsers(pageable);
        
        log.info("Returning {} users out of {} total", 
                users.getNumberOfElements(), users.getTotalElements());
        
        return users.map(this::convertToProfileDTO);
    }

    @Override
    public UserProfileDTO getUserById(Long userId, String currentUserEmail) {
        log.info("Admin {} requesting user by ID: {}", currentUserEmail, userId);
        
        // Verify admin role
        verifyAdminRole(currentUserEmail);

        // Find user by ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", userId);
                    return new UserNotFoundException("User not found with ID: " + userId);
                });

        // Check if soft deleted
        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User not found");
        }

        return convertToProfileDTO(user);
    }

    @Override
    public UserProfileDTO banUser(Long userId, boolean isBanned, String currentUserEmail) {
        log.info("Admin {} attempting to {} user with ID: {}", 
                currentUserEmail, isBanned ? "ban" : "unban", userId);
        
        // Verify admin role
        verifyAdminRole(currentUserEmail);

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Check if soft deleted
        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User not found");
        }

        // Update status
        if (isBanned) {
            user.setStatus(UserStatus.BANNED);
            log.info("Admin {} banned user: {}", currentUserEmail, user.getEmail());
        } else {
            user.setStatus(UserStatus.ACTIVE);
            log.info("Admin {} unbanned user: {}", currentUserEmail, user.getEmail());
        }

        User updatedUser = userRepository.save(user);
        return convertToProfileDTO(updatedUser);
    }

    @Override
    public AdminStatisticsDTO getUserStatistics(String currentUserEmail) {
        log.info("Admin {} requesting user statistics", currentUserEmail);
        
        // Verify admin role
        verifyAdminRole(currentUserEmail);

        // Calculate date ranges
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // Calculate statistics using repository queries
        Long totalUsers = userRepository.count();
        Long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        Long inactiveUsers = userRepository.countByStatus(UserStatus.INACTIVE);
        Long bannedUsers = userRepository.countByStatus(UserStatus.BANNED);
        Long adminCount = userRepository.countByRole(Role.ADMIN);
        Long regularUserCount = userRepository.countByRole(Role.USER);
        Long newUsersThisMonth = userRepository.countNewUsersThisMonth();
        Long newUsersThisWeek = userRepository.countNewUsersThisWeek(sevenDaysAgo);
        Long usersActiveThisWeek = userRepository.countUsersActiveAfter(sevenDaysAgo);
        Long usersActiveThisMonth = userRepository.countUsersActiveThisMonth();
        
        log.info("Statistics calculated - Total: {}, Active: {}, Banned: {}, Admins: {}", 
                totalUsers, activeUsers, bannedUsers, adminCount);
        
        return AdminStatisticsDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .bannedUsers(bannedUsers)
                .adminCount(adminCount)
                .regularUserCount(regularUserCount)
                .newUsersThisMonth(newUsersThisMonth)
                .newUsersThisWeek(newUsersThisWeek)
                .usersActiveThisWeek(usersActiveThisWeek)
                .usersActiveThisMonth(usersActiveThisMonth)
                .build();
    }

    @Override
    public Page<UserProfileDTO> searchUsers(String searchQuery, String status, String role,
                                           Pageable pageable, String currentUserEmail) {
        log.info("Admin {} searching users with query: '{}', status: {}, role: {}", 
                currentUserEmail, searchQuery, status, role);
        
        verifyAdminRole(currentUserEmail);

        Page<User> results = userRepository.findAllActiveUsers(pageable);
        
        List<User> filteredList = results.getContent().stream()
            .filter(user -> {
                boolean matchesQuery = searchQuery == null || 
                                     user.getName().toLowerCase().contains(searchQuery.toLowerCase()) ||
                                     user.getEmail().toLowerCase().contains(searchQuery.toLowerCase());
                
                boolean matchesStatus = status == null || 
                                      user.getStatus().toString().equals(status);
                
                boolean matchesRole = role == null || 
                                    user.getRole().toString().equals(role);
                
                return matchesQuery && matchesStatus && matchesRole;
            })
            .collect(Collectors.toList());

        Page<User> filtered = new PageImpl<>(filteredList, pageable, results.getTotalElements());
        
        return filtered.map(this::convertToProfileDTO);
    }

    @Override
    public List<WorkspaceResponseDTO> getAllWorkspaces(String currentUserEmail) {
        log.info("Admin {} requesting all workspaces", currentUserEmail);
        
        // Verify admin role
        verifyAdminRole(currentUserEmail);
        
        // Delegate to workspace service
        return workspaceService.getAllWorkspaces(currentUserEmail);
    }

    /**
     * Helper method to convert User entity to UserProfileDTO
     */
    private UserProfileDTO convertToProfileDTO(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .bio(user.getBio())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    @Override
    public WorkspaceStatisticsDTO getWorkspaceStatistics(String currentUserEmail) {
        log.info("Admin {} requesting workspace statistics", currentUserEmail);

        // Verify admin
        verifyAdminRole(currentUserEmail);

        Long totalWorkspaces = workspaceRepository.count();

        Long totalMembers = workspaceMemberRepository.countActiveMembers();

        Double avgMembers = totalWorkspaces == 0 ? 0.0 :
                (double) totalMembers / totalWorkspaces;

        return WorkspaceStatisticsDTO.builder()
                .totalWorkspaces(totalWorkspaces)
                .totalMembers(totalMembers)
                .averageMembersPerWorkspace(avgMembers)
                .build();
    }

    @Override
    public WorkspaceResponseDTO suspendWorkspace(Long workspaceId, String reason, String currentUserEmail) {
        log.info("Admin {} attempting to suspend workspace with ID: {}", currentUserEmail, workspaceId);

        // Verify admin role
        verifyAdminRole(currentUserEmail);

        // Find the workspace
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> {
                    log.warn("Workspace not found with ID: {}", workspaceId);
                    return new IllegalArgumentException("Workspace not found with ID: " + workspaceId);
                });

        // Check if already suspended
        if (workspace.getSuspended()) {
            log.warn("Attempted to suspend already suspended workspace ID: {}", workspaceId);
            throw new IllegalArgumentException("Workspace is already suspended");
        }

        // Suspend the workspace
        workspace.setSuspended(true);
        workspace.setSuspendedAt(LocalDateTime.now());
        workspace.setSuspensionReason(reason);

        // Save the updated workspace
        Workspace updatedWorkspace = workspaceRepository.save(workspace);
        log.info("Admin {} suspended workspace ID: {} with reason: {}", currentUserEmail, workspaceId, reason);

        // Return the updated workspace as DTO using the helper function
        return workspaceService.convertWorkspaceToDTO(updatedWorkspace);
    }

    @Override
    public WorkspaceResponseDTO unsuspendWorkspace(Long workspaceId, String currentUserEmail) {
        log.info("Admin {} attempting to unsuspend workspace with ID: {}", currentUserEmail, workspaceId);

        // Verify admin role
        verifyAdminRole(currentUserEmail);

        // Find the workspace
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> {
                    log.warn("Workspace not found with ID: {}", workspaceId);
                    return new IllegalArgumentException("Workspace not found with ID: " + workspaceId);
                });

        // Check if currently suspended
        if (!workspace.getSuspended()) {
            log.warn("Attempted to unsuspend non-suspended workspace ID: {}", workspaceId);
            throw new IllegalArgumentException("Workspace is not suspended");
        }

        // Unsuspend the workspace
        workspace.setSuspended(false);
        workspace.setSuspendedAt(null);
        workspace.setSuspensionReason(null);

        // Save the updated workspace
        Workspace updatedWorkspace = workspaceRepository.save(workspace);
        log.info("Admin {} unsuspended workspace ID: {}", currentUserEmail, workspaceId);

        // Return the updated workspace as DTO using the helper function
        return workspaceService.convertWorkspaceToDTO(updatedWorkspace);
    }
}
