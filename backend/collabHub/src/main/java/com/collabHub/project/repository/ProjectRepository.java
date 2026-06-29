package com.collabHub.project.repository;

import com.collabHub.project.entity.Project;
import com.collabHub.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** All projects in a workspace — used for listing by workspace. */
    List<Project> findByWorkspace(Workspace workspace);

    /** Look up a project by its unique key within a workspace (e.g. "COLL" in workspace 5). */
    Optional<Project> findByProjectKeyAndWorkspace(String projectKey, Workspace workspace);

    /** Guard against duplicate keys inside the same workspace before saving. */
    boolean existsByProjectKeyAndWorkspace(String projectKey, Workspace workspace);

    /** Guard against duplicate names inside the same workspace before saving. */
    boolean existsByNameAndWorkspace(String name, Workspace workspace);

    /**
     * All projects across every workspace the given user belongs to.
     * Used for "my projects" listing — mirrors the pattern in ChannelServiceImpl.getChannelsByUser().
     */
    @Query("SELECT p FROM Project p WHERE p.workspace.id IN :workspaceIds")
    List<Project> findByWorkspaceIdIn(@Param("workspaceIds") List<Long> workspaceIds);
}
