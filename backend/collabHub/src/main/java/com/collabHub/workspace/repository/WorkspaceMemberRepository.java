package com.collabHub.workspace.repository;

import com.collabHub.workspace.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    /**
     * Find a workspace member by workspace ID and user ID
     * Returns only active members (not removed)
     */
    Optional<WorkspaceMember> findByWorkspaceIdAndUserIdAndRemovedAtIsNull(Long workspaceId, Long userId);

    /**
     * Find a workspace member by workspace ID and user ID
     * Returns members regardless of status
     */
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    /**
     * Get all active members of a workspace
     * @param workspaceId workspace ID
     * @return list of active members
     */
    List<WorkspaceMember> findByWorkspaceIdAndRemovedAtIsNull(Long workspaceId);

    /**
     * Get all members (including removed) of a workspace
     * @param workspaceId workspace ID
     * @return list of all members
     */
    List<WorkspaceMember> findByWorkspaceId(Long workspaceId);

    /**
     * Check if a user is an active member of a workspace
     * @param workspaceId workspace ID
     * @param userId user ID
     * @return true if user is active member, false otherwise
     */
    boolean existsByWorkspaceIdAndUserIdAndRemovedAtIsNull(Long workspaceId, Long userId);

    /**
     * Get all workspaces a user is an active member of
     * @param userId user ID
     * @return list of workspace IDs
     */
    List<WorkspaceMember> findByUserIdAndRemovedAtIsNull(Long userId);

    /**
     * Count active members in a workspace
     * @param workspaceId workspace ID
     * @return count of active members
     */
    long countByWorkspaceIdAndRemovedAtIsNull(Long workspaceId);

    @Query("SELECT COUNT(wm) FROM WorkspaceMember wm WHERE wm.removedAt IS NULL")
    long countActiveMembers();
}
