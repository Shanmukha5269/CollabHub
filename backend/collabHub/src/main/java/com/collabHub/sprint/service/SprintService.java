package com.collabHub.sprint.service;

import com.collabHub.sprint.dto.BoardResponseDTO;
import com.collabHub.sprint.dto.CreateSprintDTO;
import com.collabHub.sprint.dto.SprintResponseDTO;
import com.collabHub.sprint.dto.UpdateSprintDTO;

import java.util.List;

public interface SprintService {

    SprintResponseDTO createSprint(Long projectId, CreateSprintDTO dto, String userEmail);

    SprintResponseDTO getSprintById(Long sprintId, String userEmail);

    List<SprintResponseDTO> getSprintsByProject(Long projectId, String userEmail);

    SprintResponseDTO updateSprint(Long sprintId, UpdateSprintDTO dto, String userEmail);

    /** Transition sprint: PLANNING → ACTIVE. Only one sprint can be ACTIVE per project. */
    SprintResponseDTO startSprint(Long sprintId, String userEmail);

    /** Transition sprint: ACTIVE → COMPLETED. Moves unfinished issues back to backlog. */
    SprintResponseDTO completeSprint(Long sprintId, String userEmail);

    /** Add an existing issue to this sprint (moves it out of backlog). */
    SprintResponseDTO addIssueToSprint(Long sprintId, Long issueId, String userEmail);

    /** Remove an issue from the sprint (moves it back to backlog). */
    SprintResponseDTO removeIssueFromSprint(Long sprintId, Long issueId, String userEmail);

    /**
     * Board view — issues of the active sprint grouped by status.
     * If no active sprint, shows backlog issues grouped by status.
     */
    BoardResponseDTO getBoardByProject(Long projectId, String userEmail);

    void deleteSprint(Long sprintId, String userEmail);
}
