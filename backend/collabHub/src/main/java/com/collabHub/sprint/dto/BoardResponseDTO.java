package com.collabHub.sprint.dto;

import com.collabHub.issue.dto.IssueResponseDTO;
import lombok.*;

import java.util.List;

/**
 * Board response — issues grouped by status column.
 *
 * This is what the frontend renders as a Kanban board.
 * Each field is a list of issues in that status column,
 * ordered by position ASC (drag-and-drop order).
 *
 * Why a dedicated DTO and not Map<String, List<IssueResponseDTO>>?
 * 1. Strong typing — the frontend knows exactly which columns exist.
 * 2. Extra board-level metadata (sprint info, project info) fits naturally.
 * 3. Easier to add new columns later without changing the contract.
 *
 * Example JSON response:
 * {
 *   "projectId": 5,
 *   "projectName": "CollabHub",
 *   "activeSprint": { "id": 2, "name": "Sprint 1", ... },
 *   "todo":        [ { issueKey: "COLL-3", ... }, ... ],
 *   "inProgress":  [ { issueKey: "COLL-1", ... }, ... ],
 *   "inReview":    [ { issueKey: "COLL-4", ... }, ... ],
 *   "done":        [ { issueKey: "COLL-2", ... }, ... ]
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardResponseDTO {

    private Long projectId;
    private String projectName;
    private String projectKey;

    /**
     * The currently active sprint for this project.
     * Null if no sprint is active — board shows backlog issues instead.
     */
    private SprintResponseDTO activeSprint;

    /** Issues in TODO column, ordered by position. */
    private List<IssueResponseDTO> todo;

    /** Issues in IN_PROGRESS column, ordered by position. */
    private List<IssueResponseDTO> inProgress;

    /** Issues in IN_REVIEW column, ordered by position. */
    private List<IssueResponseDTO> inReview;

    /** Issues in DONE column, ordered by position. */
    private List<IssueResponseDTO> done;

    /** Total issue count across all columns — derived, not stored. */
    private Integer totalIssues;
}
