package com.collabHub.project.entity;

import com.collabHub.user.entity.User;
import com.collabHub.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * Short uppercase key used to prefix issue IDs: e.g. "COLL" → COLL-1, COLL-2
     * Must be unique within the workspace.
     */
    @Column(nullable = false, length = 10)
    private String projectKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    /**
     * Project lead — the user responsible for this project.
     * Nullable: a project can exist without a dedicated lead.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private User lead;

    /**
     * User who created this project.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    /**
     * Auto-incrementing counter used to generate issue keys (COLL-1, COLL-2, …).
     * Incremented atomically inside a @Transactional block when a new issue is created.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer issueCounter = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
