package com.dmme.web;

import com.dmme.domain.AppUser;
import com.dmme.repository.AppUserRepository;
import com.dmme.repository.SubscriptionRepository;
import com.dmme.service.CurrentUserService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentUserService currentUser;
    private final SubscriptionRepository subscriptions;
    private final AppUserRepository users;

    public MeController(CurrentUserService currentUser, SubscriptionRepository subscriptions,
                        AppUserRepository users) {
        this.currentUser = currentUser;
        this.subscriptions = subscriptions;
        this.users = users;
    }

    @GetMapping
    public Map<String, Object> me() {
        AppUser u = currentUser.require();
        Map<String, Object> out = new HashMap<>();
        out.put("id", u.getId());
        out.put("email", u.getEmail());
        out.put("fullName", u.getFullName());
        out.put("planCode", u.getPlanCode());
        subscriptions.findFirstByUserIdOrderByCreatedAtDesc(u.getId()).ifPresent(s -> {
            out.put("subscriptionStatus", s.getStatus());
            out.put("currentPeriodEnd", s.getCurrentPeriodEnd());
        });
        return out;
    }

    public record ProfileUpdate(String fullName) {}

    @PutMapping
    public Map<String, Object> update(@RequestBody ProfileUpdate req) {
        AppUser u = currentUser.require();
        if (req.fullName() != null) u.setFullName(req.fullName());
        users.save(u);
        return me();
    }
}
