package com.collabHub.channel.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelResponseDTO {

    private Long id;
    private String name;
    private String description;
    private Long workspaceId;
    private String workspaceName;
    private Boolean isPrivate;
    private Long creatorId;
    private String creatorName;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}