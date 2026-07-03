package com.dmme.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String planCode;

    @Column(unique = true)
    private String razorpaySubscriptionId;

    /** CREATED | ACTIVE | HALTED | CANCELLED | EXPIRED */
    @Column(nullable = false)
    private String status = "CREATED";

    private Instant currentPeriodEnd;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
