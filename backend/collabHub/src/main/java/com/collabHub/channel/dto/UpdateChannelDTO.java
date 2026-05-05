package com.collabHub.channel.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateChannelDTO {

    @Size(min = 1, max = 80, message = "Channel name must be between 1 and 80 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean isPrivate;
}