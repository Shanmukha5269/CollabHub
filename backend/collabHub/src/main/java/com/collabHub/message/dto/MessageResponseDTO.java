package com.collabHub.message.dto;

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

    /**
     * Issue key this message is linked to (e.g. "COLL-1").
     * Null if the message is not related to any issue.
     * Frontend uses this to show an issue card preview inside the message.
     */
    private String relatedIssueKey;

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
