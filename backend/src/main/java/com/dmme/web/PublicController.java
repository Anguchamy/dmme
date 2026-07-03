package com.dmme.web;

import com.dmme.domain.Plan;
import com.dmme.repository.PlanRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Unauthenticated endpoints used by the marketing site. */
@RestController
@RequestMapping("/api")
public class PublicController {

    private final PlanRepository plans;

    public PublicController(PlanRepository plans) {
        this.plans = plans;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/plans")
    public List<Plan> plans() {
        return plans.findAllByOrderBySortOrderAsc();
    }
}
