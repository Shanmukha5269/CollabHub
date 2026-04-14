package com.collabHub.user.service;

import com.collabHub.user.dto.UserProfileDTO;
import com.collabHub.user.dto.UserRequestDTO;
import com.collabHub.user.dto.UserResponseDTO;
import com.collabHub.user.dto.UserUpdateDTO;

public interface UserService {

    UserResponseDTO registerUser(UserRequestDTO userRequestDTO);

    UserProfileDTO getUserProfile(Long userId, String currentUserEmail);

    UserProfileDTO updateUserProfile(Long userId, UserUpdateDTO userUpdateDTO, String currentUserEmail);

    void deleteUser(Long userId, String currentUserEmail);

    UserProfileDTO getUserById(Long userId);
}