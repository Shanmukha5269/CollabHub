package com.collabHub.user.service;

import com.collabHub.user.dto.UserRequestDTO;
import com.collabHub.user.dto.UserResponseDTO;

public interface UserService {

    UserResponseDTO registerUser(UserRequestDTO userRequestDTO);
}