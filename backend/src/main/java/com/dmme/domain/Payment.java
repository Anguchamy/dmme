package com.dmme.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private String razorpayOrderId;
    private String razorpayPaymentId;

    @Column(nullable = false)
    private int amountMinor;

    @Column(nullable = false)
    private String currency = "INR";

    /** CREATED | PAID | FAILED | REFUNDED */
    @Column(nullable = false)
    private String status = "CREATED";

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
