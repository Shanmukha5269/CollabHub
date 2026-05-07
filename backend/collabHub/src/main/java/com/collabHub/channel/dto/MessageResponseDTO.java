package com.collabHub.channel.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponseDTO {

    private Long id;
    private String content;
    private Long channelId;
    private String channelName;
    private Long senderId;
    private String senderName;
    private String senderEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isEdited;
    private Map<String, Integer> reactions;
    private Set<UserMinimalDTO> mentions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserMinimalDTO {
        private Long id;
        private String name;
        private String email;
    }
}
