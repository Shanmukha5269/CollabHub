package com.collabHub.channel.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelMemberResponseDTO {

    private Long memberId;
    private String memberName;
    private String memberEmail;
    private Long channelId;
    private String channelName;
    private Long workspaceId;
    private String workspaceName;
    private Boolean isChannelCreator;
    private Boolean isWorkspaceOwner;
    private LocalDateTime addedAt;
}
