package com.dmme.repository;

import com.dmme.domain.FlowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlowStepRepository extends JpaRepository<FlowStep, Long> {
    List<FlowStep> findByAutomationIdOrderByStepOrderAsc(Long automationId);
    void deleteByAutomationId(Long automationId);
}
