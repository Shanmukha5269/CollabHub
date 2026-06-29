package com.collabHub.issue.repository;

import com.collabHub.issue.entity.Issue;
import com.collabHub.issue.entity.IssueStatus;
import com.collabHub.issue.entity.IssuePriority;
import com.collabHub.issue.entity.IssueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    /**
     * All issues for a project — paginated, ordered by position then createdAt.
     * Mirrors MessageRepository.findByChannelIdPaginated exactly.
     * This is the main listing query used by the backlog and board views.
     */
    @Query("SELECT i FROM Issue i WHERE i.project.id = :projectId ORDER BY i.position ASC, i.createdAt DESC")
    Page<Issue> findByProjectIdPaginated(@Param("projectId") Long projectId, Pageable pageable);

    /**
     * Filter issues by project + status — used for board column queries.
     * e.g. "all IN_PROGRESS issues for project 5"
     */
    @Query("SELECT i FROM Issue i WHERE i.project.id = :projectId AND i.status = :status ORDER BY i.position ASC")
    Page<Issue> findByProjectIdAndStatus(
            @Param("projectId") Long projectId,
            @Param("status") IssueStatus status,
            Pageable pageable);

    /**
     * Filter issues by project + priority.
     */
    @Query("SELECT i FROM Issue i WHERE i.project.id = :projectId AND i.priority = :priority ORDER BY i.position ASC, i.createdAt DESC")
    Page<Issue> findByProjectIdAndPriority(
            @Param("projectId") Long projectId,
            @Param("priority") IssuePriority priority,
            Pageable pageable);

    /**
     * Filter issues by project + type.
     */
    @Query("SELECT i FROM Issue i WHERE i.project.id = :projectId AND i.type = :type ORDER BY i.position ASC, i.createdAt DESC")
    Page<Issue> findByProjectIdAndType(
            @Param("projectId") Long projectId,
            @Param("type") IssueType type,
            Pageable pageable);

    /**
     * All issues assigned to a specific user within a project.
     */
    @Query("SELECT i FROM Issue i WHERE i.project.id = :projectId AND i.assignee.id = :assigneeId ORDER BY i.position ASC")
    Page<Issue> findByProjectIdAndAssigneeId(
            @Param("projectId") Long projectId,
            @Param("assigneeId") Long assigneeId,
            Pageable pageable);

    /**
     * Look up by the human-readable key (e.g. "COLL-1").
     * Used by GET /issues/key/{key} endpoint.
     */
    Optional<Issue> findByIssueKey(String issueKey);

    /**
     * Count total issues in a project — used to verify issueCounter is in sync.
     */
    @Query("SELECT COUNT(i) FROM Issue i WHERE i.project.id = :projectId")
    Long countByProjectId(@Param("projectId") Long projectId);
}
