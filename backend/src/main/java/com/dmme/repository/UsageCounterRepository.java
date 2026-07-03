package com.dmme.repository;

import com.dmme.domain.UsageCounter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageCounterRepository extends JpaRepository<UsageCounter, UsageCounter.Key> {
}
