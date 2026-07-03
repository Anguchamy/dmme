package com.dmme.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Tracks where a follower is inside a multi-step DM flow. */
@Getter
@Setter
@Entity
@Table(name = "conversation_state")
public class ConversationState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long igAccountId;

    @Column(nullable = false)
    private String igUserId;

    @Column(nullable = false)
    private Long automationId;

    @Column(nullable = false)
    private int currentStep = 0;

    /** The lead field we just asked the follower for (awaiting their reply). */
    private String awaitingField;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
