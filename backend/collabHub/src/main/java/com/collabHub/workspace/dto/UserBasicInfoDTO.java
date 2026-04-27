package com.collabHub.workspace.dto;

import com.collabHub.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal user information for embedded owner/member data
 * Only includes essential fields - no null values
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBasicInfoDTO {

    private Long id;
    private String name;
    private String email;
    private Role role;
}
