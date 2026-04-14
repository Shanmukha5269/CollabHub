package com.collabHub.auth.dto;

import com.collabHub.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Login Response DTO
 * Returned after successful login with JWT token and user info
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private String token;
    private String refreshToken;
    private UserInfoDTO user;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfoDTO {
        private Long id;
        private String name;
        private String email;
        private Role role;
    }
}
