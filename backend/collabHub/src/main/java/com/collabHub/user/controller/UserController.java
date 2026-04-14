package com.collabHub.user.controller;

import com.collabHub.common.util.SecurityUtil;
import com.collabHub.user.dto.UserProfileDTO;
import com.collabHub.user.dto.UserRequestDTO;
import com.collabHub.user.dto.UserResponseDTO;
import com.collabHub.user.dto.UserUpdateDTO;
import com.collabHub.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User Controller
 * Handles user management endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Register a new user
     * POST /api/users/register
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> registerUser(@Valid @RequestBody UserRequestDTO userRequestDTO) {
        log.info("User registration request for email: {}", userRequestDTO.getEmail());
        UserResponseDTO response = userService.registerUser(userRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get user profile
     * Permission:
     * - USER: Can only view their own profile
     * - ADMIN: Can view any user profile
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable Long id) {
        log.info("Get user profile request for user id: {}", id);
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        UserProfileDTO profileDTO = userService.getUserProfile(id, currentUserEmail);
        return ResponseEntity.ok(profileDTO);
    }

    /**
     * Update user profile
     * Permission:
     * - USER: Can only update their own profile
     * - ADMIN: Can update any user profile
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserProfileDTO> updateUserProfile(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        log.info("Update user profile request for user id: {}", id);
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        UserProfileDTO profileDTO = userService.updateUserProfile(id, userUpdateDTO, currentUserEmail);
        return ResponseEntity.ok(profileDTO);
    }

    /**
     * Delete user account (soft delete)
     * Permission:
     * - USER: Can only delete their own account
     * - ADMIN: Can delete any user account
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        log.info("Delete user request for user id: {}", id);
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        userService.deleteUser(id, currentUserEmail);
        return ResponseEntity.ok("User account deleted successfully");
    }
}