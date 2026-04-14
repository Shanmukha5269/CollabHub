package com.collabHub.user.service;

import com.collabHub.common.exception.UserAlreadyExistsException;
import com.collabHub.user.dto.UserRequestDTO;
import com.collabHub.user.dto.UserResponseDTO;
import com.collabHub.user.entity.User;
import com.collabHub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
                .build();

        User savedUser = userRepository.save(user);

        // Convert Entity → Response DTO
        return UserResponseDTO.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }
}