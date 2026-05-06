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
    private String creatorEmail;
    private Long workspaceOwnerId;
    private String workspaceOwnerName;
    private String workspaceOwnerEmail;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}