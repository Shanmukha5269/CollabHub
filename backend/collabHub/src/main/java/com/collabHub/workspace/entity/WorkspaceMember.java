package com.collabHub.workspace.entity;

import com.collabHub.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 When a workspace is created, the creator is automatically OWNER
 OWNER will also be added as a member to workspace
 */
@Entity
@Table(name = "workspace_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"workspace_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the Workspace
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    /**
     * Reference to the User
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Role of the user within this workspace
     * Can be MEMBER or OWNER
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WorkspaceRole role = WorkspaceRole.MEMBER;

    /**
     * Whether this member can manage other members in this workspace
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean canManageMembers = false;

    /**
     * Timestamp when the user joined/was added to this workspace
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    /**
     * Soft delete timestamp
     * If not null, the user has been removed from this workspace
     */
    @Column(nullable = true)
    private LocalDateTime removedAt;
}
