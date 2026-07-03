package com.dmme.repository;

import com.dmme.domain.TriggerKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TriggerKeywordRepository extends JpaRepository<TriggerKeyword, Long> {
    List<TriggerKeyword> findByAutomationId(Long automationId);
    void deleteByAutomationId(Long automationId);
}
