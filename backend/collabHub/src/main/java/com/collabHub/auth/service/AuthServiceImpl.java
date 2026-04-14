package com.collabHub.auth.service;

import com.collabHub.auth.dto.LoginRequestDTO;
import com.collabHub.auth.dto.LoginResponseDTO;
import com.collabHub.auth.security.JwtTokenProvider;
import com.collabHub.common.exception.UserNotFoundException;
import com.collabHub.user.entity.User;
import com.collabHub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Auth Service Implementation
 * Handles authentication logic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        log.info("Login attempt for email: {}", loginRequestDTO.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(loginRequestDTO.getEmail())
                .orElseThrow(() -> {
                    log.warn("User not found: {}", loginRequestDTO.getEmail());
                    return new UserNotFoundException("User not found");
                });

        // Validate password
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", loginRequestDTO.getEmail());
            throw new UserNotFoundException("Invalid email or password");
        }

        log.info("Login successful for user: {}", loginRequestDTO.getEmail());

        // Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String token = jwtTokenProvider.generateTokenFromEmail(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // Build response
        return LoginResponseDTO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(LoginResponseDTO.UserInfoDTO.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();
    }

    @Override
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    @Override
    public String getEmailFromToken(String token) {
        return jwtTokenProvider.getEmailFromToken(token);
    }
}
