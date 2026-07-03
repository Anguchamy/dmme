package com.dmme.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    private UUID id;               // == Supabase auth uid

    @Column(nullable = false, unique = true)
    private String email;

    private String fullName;
    private String avatarUrl;

    @Column(nullable = false)
    private String planCode = "FREE";

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
