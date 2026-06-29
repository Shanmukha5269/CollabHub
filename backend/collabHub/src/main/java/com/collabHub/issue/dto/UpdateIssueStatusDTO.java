package com.collabHub.issue.dto;

import com.collabHub.issue.entity.IssueStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateIssueStatusDTO {

    /**
     * The new status to transition this issue to.
     * Must be one of: TODO, IN_PROGRESS, IN_REVIEW, DONE
     *
     * Why a separate DTO?
     * Status changes are very frequent (drag on board, click in detail view)
     * and deserve their own endpoint rather than going through the full update flow.
     * This is the same reason Jira has a dedicated "transition" API.
     */
    @NotNull(message = "Status is required")
    private IssueStatus status;
}
