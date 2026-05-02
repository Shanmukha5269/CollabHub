package com.collabHub.workspace.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO for updating a member's role in a workspace
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferOwnershipDTO {

    @NotNull(message = "New owner userId is required")
    private Long newOwnerUserId;
}
