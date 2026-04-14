package com.collabHub.auth.controller;

import com.collabHub.auth.dto.AuthResponseDTO;
import com.collabHub.auth.dto.LoginRequestDTO;
import com.collabHub.auth.dto.LoginResponseDTO;
import com.collabHub.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth Controller
 * Handles authentication endpoints (login, logout, etc.)
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        log.info("Login request received for email: {}", loginRequestDTO.getEmail());

        try {
            LoginResponseDTO response = authService.login(loginRequestDTO);
            
            AuthResponseDTO authResponse = AuthResponseDTO.builder()
                    .success(true)
                    .message("Login successful")
                    .data(response)
                    .status(HttpStatus.OK.value())
                    .build();

            log.info("Login successful for email: {}", loginRequestDTO.getEmail());
            return ResponseEntity.ok(authResponse);

        } catch (Exception ex) {
            log.error("Login failed: {}", ex.getMessage());
            
            AuthResponseDTO errorResponse = AuthResponseDTO.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .build();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
}
