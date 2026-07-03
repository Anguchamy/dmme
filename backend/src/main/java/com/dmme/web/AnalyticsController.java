package com.dmme.web;

import com.dmme.repository.LeadRepository;
import com.dmme.repository.MessageLogRepository;
import com.dmme.service.CurrentUserService;
import com.dmme.service.UsageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final MessageLogRepository logs;
    private final LeadRepository leads;
    private final UsageService usage;
    private final CurrentUserService currentUser;

    public AnalyticsController(MessageLogRepository logs, LeadRepository leads,
                               UsageService usage, CurrentUserService currentUser) {
        this.logs = logs;
        this.leads = leads;
        this.usage = usage;
        this.currentUser = currentUser;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        UUID userId = currentUser.userId();
        Instant last30 = Instant.now().minus(30, ChronoUnit.DAYS);
        return Map.of(
                "dmsSent30d", logs.countByUserIdAndDirectionAndCreatedAtAfter(userId, "OUT", last30),
                "dmsReceived30d", logs.countByUserIdAndDirectionAndCreatedAtAfter(userId, "IN", last30),
                "totalLeads", leads.countByUserId(userId),
                "dmsUsedThisMonth", usage.usedThisMonth(userId)
        );
    }
}
