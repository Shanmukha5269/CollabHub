package com.collabHub.auth.service;

import com.collabHub.auth.dto.LoginRequestDTO;
import com.collabHub.auth.dto.LoginResponseDTO;

/**
 * Auth Service Interface
 * Defines authentication operations
 */
public interface AuthService {

    LoginResponseDTO login(LoginRequestDTO loginRequestDTO);

    boolean validateToken(String token);

    String getEmailFromToken(String token);
}
