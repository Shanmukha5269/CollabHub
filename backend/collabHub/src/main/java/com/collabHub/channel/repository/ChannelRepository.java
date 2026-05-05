package com.collabHub.channel.repository;

import com.collabHub.channel.entity.Channel;
import com.collabHub.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    List<Channel> findByWorkspace(Workspace workspace);

    List<Channel> findByWorkspaceAndIsPrivate(Workspace workspace, Boolean isPrivate);

    Optional<Channel> findByNameAndWorkspace(String name, Workspace workspace);

    @Query("SELECT c FROM Channel c JOIN c.members m WHERE m.id = :userId AND c.workspace.id = :workspaceId")
    List<Channel> findByMemberAndWorkspace(@Param("userId") Long userId, @Param("workspaceId") Long workspaceId);

    boolean existsByNameAndWorkspace(String name, Workspace workspace);
}