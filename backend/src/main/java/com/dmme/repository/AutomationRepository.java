package com.dmme.repository;

import com.dmme.domain.Automation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutomationRepository extends JpaRepository<Automation, Long> {
    List<Automation> findByUserId(UUID userId);
    Optional<Automation> findByIdAndUserId(Long id, UUID userId);
    List<Automation> findByIgAccountIdAndTypeAndStatus(Long igAccountId, String type, String status);
}
