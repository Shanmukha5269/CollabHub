package com.collabHub.message.dto;

import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMessageDTO {

    @Size(min = 1, max = 4000, message = "Message must be between 1 and 4000 characters")
    private String content;

    @Builder.Default
    private Set<Long> mentionedUserIds = Set.of();
}
