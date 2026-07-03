package com.dmme.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "lead")
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private Long automationId;

    @Column(nullable = false)
    private String igUserId;

    private String igUsername;
    private String email;
    private String phone;
    private String name;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> customData = new HashMap<>();

    private Boolean didFollow;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
