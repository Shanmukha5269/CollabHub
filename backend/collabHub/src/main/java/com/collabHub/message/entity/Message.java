package com.collabHub.message.entity;

import com.collabHub.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.collabHub.channel.entity.Channel;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * Optional reference to a Jira issue by its key (e.g. "COLL-1").
     *
     * WHY a String key and not a FK to the Issue table?
     * A FK would tightly couple the Message module to the Issue module.
     * If an issue is deleted, the FK would break or cascade-delete messages.
     * Using the key as a plain String keeps the modules independent —
     * the same way Slack stores issue references as text, not DB relations.
     * The frontend uses this key to fetch the issue separately if needed.
     *
     * Nullable — most messages won't be linked to an issue.
     */
    @Column(name = "related_issue_key", length = 20)
    private String relatedIssueKey;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isEdited = false;

    @ElementCollection
    @CollectionTable(name = "message_reactions", joinColumns = @JoinColumn(name = "message_id"))
    @MapKeyColumn(name = "emoji")
    @Column(name = "reaction_count")
    @Builder.Default
    private Map<String, Integer> reactions = new HashMap<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "message_mentions",
            joinColumns = @JoinColumn(name = "message_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> mentions = new HashSet<>();
}
