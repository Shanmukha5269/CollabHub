package com.collabHub.sprint.repository;

import com.collabHub.sprint.entity.Sprint;
import com.collabHub.sprint.entity.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {

    /** All sprints for a project, newest first. */
    List<Sprint> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    /**
     * Find the single ACTIVE sprint for a project.
     * Used by the board endpoint and as the guard in startSprint()
     * to enforce the "only one active sprint per project" rule.
     */
    Optional<Sprint> findByProjectIdAndStatus(Long projectId, SprintStatus status);

    /**
     * Check if a project already has an active sprint.
     * Used as a guard before calling startSprint().
     */
    boolean existsByProjectIdAndStatus(Long projectId, SprintStatus status);

    /**
     * All issues belonging to a sprint, grouped by status.
     * Used by the board endpoint — fetches issues with their
     * reporter/assignee/project eagerly to avoid N+1 queries.
     */
    @Query("SELECT s FROM Sprint s " +
           "LEFT JOIN FETCH s.project " +
           "LEFT JOIN FETCH s.creator " +
           "WHERE s.id = :sprintId")
    Optional<Sprint> findByIdWithDetails(@Param("sprintId") Long sprintId);
}
