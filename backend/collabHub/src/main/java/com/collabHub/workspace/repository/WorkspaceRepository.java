package com.collabHub.workspace.repository;

import com.collabHub.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    /**
     * Find workspace by ID and Owner ID
     * Ensures user can only access their own workspaces
     */
    Optional<Workspace> findByIdAndOwnerId(Long workspaceId, Long ownerId);

    /**
     * Find all workspaces owned by a user
     */
    List<Workspace> findByOwnerId(Long ownerId);

    /**
     * Find workspace by name and owner
     * Check for duplicate workspace names per user
     */
    Optional<Workspace> findByNameAndOwnerId(String name, Long ownerId);
}
