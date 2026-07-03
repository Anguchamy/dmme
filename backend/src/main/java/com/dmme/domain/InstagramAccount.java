package com.dmme.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "instagram_account")
public class InstagramAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String igUserId;

    private String igUsername;
    private String facebookPageId;

    @Column(length = 4000)
    private String pageAccessToken;

    private Instant tokenExpiresAt;

    @Column(nullable = false)
    private Instant connectedAt = Instant.now();

    @Column(nullable = false)
    private boolean isActive = true;
}
