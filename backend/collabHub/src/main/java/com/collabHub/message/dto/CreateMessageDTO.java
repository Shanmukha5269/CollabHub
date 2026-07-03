package com.collabHub.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMessageDTO {

    @NotBlank(message = "Message content is required")
    @Size(max = 10000, message = "Message content must not exceed 10000 characters")
    private String content;

    private List<Long> mentionedUserIds;

    /**
     * Optional issue key to link this message to a Jira issue (e.g. "COLL-1").
     * Validated to match the issue key format: 2-10 uppercase letters, dash, 1+ digits.
     * Null means no issue linked.
     */
    @Pattern(
            regexp = "^[A-Z]{2,10}-[0-9]+$",
            message = "Issue key must match format PROJ-123 (e.g. COLL-1)"
    )
    private String relatedIssueKey;
}
