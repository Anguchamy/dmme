package com.dmme.repository;

import com.dmme.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}
