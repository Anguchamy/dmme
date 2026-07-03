package com.dmme.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "automation")
public class Automation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private Long igAccountId;

    @Column(nullable = false)
    private String name;

    /** COMMENT | STORY_REPLY | LIVE | DM */
    @Column(nullable = false)
    private String type = "COMMENT";

    /** ACTIVE | PAUSED | DRAFT */
    @Column(nullable = false)
    private String status = "DRAFT";

    /** null => applies to every post/reel; otherwise a specific media id */
    private String igMediaId;

    @Column(nullable = false)
    private boolean matchAny = false;

    private String publicReply;

    @Column(nullable = false)
    private boolean askFollowEnabled = false;

    private String askFollowMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
