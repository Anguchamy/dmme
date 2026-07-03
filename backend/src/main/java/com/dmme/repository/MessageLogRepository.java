package com.dmme.repository;

import com.dmme.domain.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {
    List<MessageLog> findTop100ByUserIdOrderByCreatedAtDesc(UUID userId);
    long countByUserIdAndDirectionAndCreatedAtAfter(UUID userId, String direction, Instant after);
}
