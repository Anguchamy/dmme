package com.dmme.web;

import com.dmme.domain.InstagramAccount;
import com.dmme.repository.InstagramAccountRepository;
import com.dmme.service.CurrentUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Manage the creator's connected Instagram account(s).
 *
 * The Meta OAuth "Login with Instagram" flow runs on the frontend and returns
 * a Page + IG business account with a long-lived page access token. The client
 * posts those details here to persist the connection.
 */
@RestController
@RequestMapping("/api/instagram/accounts")
public class InstagramAccountController {

    private final InstagramAccountRepository repo;
    private final CurrentUserService currentUser;

    public InstagramAccountController(InstagramAccountRepository repo, CurrentUserService currentUser) {
        this.repo = repo;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findByUserId(currentUser.userId()).stream()
                .map(a -> Map.<String, Object>of(
                        "id", a.getId(),
                        "igUserId", a.getIgUserId(),
                        "igUsername", a.getIgUsername() != null ? a.getIgUsername() : "",
                        "isActive", a.isActive(),
                        "connectedAt", a.getConnectedAt()))
                .toList();
    }

    public record ConnectRequest(
            String igUserId, String igUsername, String facebookPageId, String pageAccessToken) {}

    @PostMapping("/connect")
    public Map<String, Object> connect(@RequestBody ConnectRequest req) {
        var user = currentUser.require();
        InstagramAccount acc = repo.findByIgUserId(req.igUserId()).orElseGet(InstagramAccount::new);
        acc.setUserId(user.getId());
        acc.setIgUserId(req.igUserId());
        acc.setIgUsername(req.igUsername());
        acc.setFacebookPageId(req.facebookPageId());
        acc.setPageAccessToken(req.pageAccessToken());
        acc.setActive(true);
        repo.save(acc);
        return Map.of("id", acc.getId(), "igUsername",
                acc.getIgUsername() != null ? acc.getIgUsername() : "");
    }

    @DeleteMapping("/{id}")
    public void disconnect(@PathVariable Long id) {
        repo.findById(id)
                .filter(a -> a.getUserId().equals(currentUser.userId()))
                .ifPresent(repo::delete);
    }
}
