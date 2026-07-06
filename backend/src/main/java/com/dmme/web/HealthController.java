package com.dmme.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight, unauthenticated liveness endpoint. Ideal target for an external
 * uptime pinger (UptimeRobot / cron-job.org) to keep the Render free instance
 * warm so it doesn't cold-start and drop incoming Instagram webhooks.
 *
 * Does not touch the database — must respond instantly.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of("status", "ok", "ts", System.currentTimeMillis());
    }
}
