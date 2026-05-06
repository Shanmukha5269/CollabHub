package com.collabHub.channel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddMemberDTO {

    @NotNull(message = "User ID is required")
    private Long userId;
}
