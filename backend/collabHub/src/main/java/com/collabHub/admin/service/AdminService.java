package com.collabHub.admin.service;

import com.collabHub.user.dto.UserProfileDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.collabHub.admin.dto.*;


public interface AdminService {

    /**
     * ADMIN ONLY: Get all users with pagination
     */
    Page<UserProfileDTO> getAllUsers(Pageable pageable, String currentUserEmail);

    /**
     * ADMIN ONLY: Get user details by ID
     */
    UserProfileDTO getUserById(Long userId, String currentUserEmail);

    /**
     * ADMIN ONLY: Ban/Unban a user
     */
    UserProfileDTO banUser(Long userId, boolean isBanned, String currentUserEmail);

    /**
     * ADMIN ONLY: Get statistics about users
     */
    AdminStatisticsDTO getUserStatistics(String currentUserEmail);

    /**
     * ADMIN ONLY: Search users with filters
     */
    Page<UserProfileDTO> searchUsers(String searchQuery, String status, String role, 
                                     Pageable pageable, String currentUserEmail);
}
