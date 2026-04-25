package com.collabHub.admin.service;

import com.collabHub.admin.dto.AdminStatisticsDTO;
import com.collabHub.common.exception.UserAccessDeniedException;
import com.collabHub.common.exception.UserNotFoundException;
import com.collabHub.user.dto.UserProfileDTO;
import com.collabHub.user.entity.Role;
import com.collabHub.user.entity.User;
import com.collabHub.user.entity.UserStatus;
import com.collabHub.user.repository.UserRepository;
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
}
