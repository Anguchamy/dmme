package com.dmme.service;

import com.dmme.domain.AppUser;
import com.dmme.domain.Plan;
import com.dmme.domain.UsageCounter;
import com.dmme.repository.PlanRepository;
import com.dmme.repository.UsageCounterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.UUID;

/** Meters outbound DMs and enforces the user's plan limit. */
@Service
public class UsageService {

    private final UsageCounterRepository counters;
    private final PlanRepository plans;

    public UsageService(UsageCounterRepository counters, PlanRepository plans) {
        this.counters = counters;
        this.plans = plans;
    }

    private String currentPeriod() {
        return YearMonth.now().toString();  // e.g. 2026-07
    }

    /** True if the user still has DM quota this month. */
    public boolean canSend(AppUser user) {
        Plan plan = plans.findById(user.getPlanCode()).orElse(null);
        if (plan == null || plan.getDmLimit() == null) {
            return true;  // unlimited plan
        }
        int used = counters.findById(new UsageCounter.Key(user.getId(), currentPeriod()))
                .map(UsageCounter::getDmsSent).orElse(0);
        return used < plan.getDmLimit();
    }

    @Transactional
    public void recordSend(UUID userId) {
        String period = currentPeriod();
        UsageCounter c = counters.findById(new UsageCounter.Key(userId, period))
                .orElseGet(() -> {
                    UsageCounter n = new UsageCounter();
                    n.setUserId(userId);
                    n.setPeriod(period);
                    n.setDmsSent(0);
                    return n;
                });
        c.setDmsSent(c.getDmsSent() + 1);
        counters.save(c);
    }

    public int usedThisMonth(UUID userId) {
        return counters.findById(new UsageCounter.Key(userId, currentPeriod()))
                .map(UsageCounter::getDmsSent).orElse(0);
    }
}
