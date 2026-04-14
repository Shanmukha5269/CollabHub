package com.collabHub.user.service;

import com.collabHub.common.exception.UserAccessDeniedException;
import com.collabHub.common.exception.UserAlreadyExistsException;
import com.collabHub.common.exception.UserNotFoundException;
import com.collabHub.user.dto.UserProfileDTO;
import com.collabHub.user.dto.UserRequestDTO;
import com.collabHub.user.dto.UserResponseDTO;
import com.collabHub.user.dto.UserUpdateDTO;
import com.collabHub.user.entity.Role;
import com.collabHub.user.entity.User;
import com.collabHub.user.entity.UserStatus;
import com.collabHub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDTO registerUser(UserRequestDTO dto) {

        // 🔐 Check email existence
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        // 🔐 Hash password
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // Convert DTO → Entity
        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(encodedPassword)
                .role(dto.getRole())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        // Convert Entity → Response DTO
        return UserResponseDTO.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .bio(savedUser.getBio())
                .role(savedUser.getRole())
                .status(savedUser.getStatus())
                .createdAt(savedUser.getCreatedAt())
                .updatedAt(savedUser.getUpdatedAt())
                .build();
    }

    @Override
    public UserProfileDTO getUserProfile(Long userId, String currentUserEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Check if user is soft deleted
        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User not found");
        }

        // Verify permission: USER can only view their own profile, ADMIN can view any
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(userId)) {
            throw new UserAccessDeniedException("You don't have permission to view this profile");
        }

        return convertToProfileDTO(user);
    }

    @Override
    public UserProfileDTO updateUserProfile(Long userId, UserUpdateDTO updateDTO, String currentUserEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Check if user is soft deleted
        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User not found");
        }

        // Verify permission: USER can only update their own profile, ADMIN can update any
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(userId)) {
            throw new UserAccessDeniedException("You don't have permission to update this profile");
        }

        // Check if new email is already taken (but allow same email)
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(updateDTO.getEmail()).isPresent()) {
                throw new UserAlreadyExistsException("Email already in use");
            }
            user.setEmail(updateDTO.getEmail());
        }

        // Update allowed fields
        if (updateDTO.getName() != null) {
            user.setName(updateDTO.getName());
        }

        if (updateDTO.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(updateDTO.getPassword()));
        }

        if (updateDTO.getBio() != null) {
            user.setBio(updateDTO.getBio());
        }

        User updatedUser = userRepository.save(user);
        return convertToProfileDTO(updatedUser);
    }

    @Override
    public void deleteUser(Long userId, String currentUserEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Check if already soft deleted
        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User not found");
        }

        // Verify permission: USER can only delete their own account, ADMIN can delete any
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(userId)) {
            throw new UserAccessDeniedException("You don't have permission to delete this account");
        }

        // Soft delete: mark as INACTIVE and set deletedAt
        user.setStatus(UserStatus.INACTIVE);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public UserProfileDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Check if user is soft deleted
        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User not found");
        }

        return convertToProfileDTO(user);
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