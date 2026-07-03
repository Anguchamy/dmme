package com.dmme.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "message_log")
public class MessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private Long automationId;
    private String igUserId;

    /** IN | OUT */
    @Column(nullable = false)
    private String direction;

    /** COMMENT | DM | STORY_REPLY */
    @Column(nullable = false)
    private String channel = "DM";

    @Column(length = 4000)
    private String content;

    /** SENT | DELIVERED | FAILED | RECEIVED */
    @Column(nullable = false)
    private String status = "SENT";

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
