package com.collabHub.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspendWorkspaceRequestDTO {

    @NotBlank(message = "Suspension reason is required")
    private String reason;
}
