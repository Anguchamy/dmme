package com.dmme.repository;

import com.dmme.domain.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, Long> {
    List<Lead> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Lead> findByUserIdAndAutomationIdAndIgUserId(UUID userId, Long automationId, String igUserId);
    long countByUserId(UUID userId);
}
