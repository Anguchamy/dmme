package com.dmme.repository;

import com.dmme.domain.ConversationState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationStateRepository extends JpaRepository<ConversationState, Long> {
    Optional<ConversationState> findByIgAccountIdAndIgUserIdAndAutomationId(
            Long igAccountId, String igUserId, Long automationId);
}
