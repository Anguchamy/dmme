package com.dmme.web;

import com.dmme.service.AutomationService;
import com.dmme.service.CurrentUserService;
import com.dmme.web.dto.AutomationDtos.AutomationUpsert;
import com.dmme.web.dto.AutomationDtos.AutomationView;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/automations")
public class AutomationController {

    private final AutomationService service;
    private final CurrentUserService currentUser;

    public AutomationController(AutomationService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<AutomationView> list() {
        currentUser.require();
        return service.list(currentUser.userId());
    }

    @GetMapping("/{id}")
    public AutomationView get(@PathVariable Long id) {
        return service.get(id, currentUser.userId());
    }

    @PostMapping
    public AutomationView create(@RequestBody AutomationUpsert req) {
        currentUser.require();
        return service.create(req, currentUser.userId());
    }

    @PutMapping("/{id}")
    public AutomationView update(@PathVariable Long id, @RequestBody AutomationUpsert req) {
        return service.update(id, req, currentUser.userId());
    }

    @PatchMapping("/{id}/status")
    public AutomationView setStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.setStatus(id, body.get("status"), currentUser.userId());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id, currentUser.userId());
    }
}
