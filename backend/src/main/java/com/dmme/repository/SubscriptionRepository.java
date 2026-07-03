package com.dmme.repository;

import com.dmme.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);
}
