package com.dmme.web;

import com.dmme.domain.InstagramAccount;
import com.dmme.repository.InstagramAccountRepository;
import com.dmme.service.CurrentUserService;
import com.dmme.service.InstagramClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

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
    private final InstagramClient instagram;

    public InstagramAccountController(InstagramAccountRepository repo, CurrentUserService currentUser,
                                      InstagramClient instagram) {
        this.repo = repo;
        this.currentUser = currentUser;
        this.instagram = instagram;
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

    /** Recent posts + reels for the connected account, for the visual post picker. */
    @GetMapping("/{id}/media")
    public List<Map<String, Object>> media(@PathVariable Long id) {
        InstagramAccount acc = repo.findById(id)
                .filter(a -> a.getUserId().equals(currentUser.userId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Instagram account not found"));

        return instagram.listMedia(acc.getPageAccessToken(), acc.getIgUserId()).stream()
                .map(m -> {
                    Object thumb = m.getOrDefault("thumbnail_url", m.get("media_url"));
                    return Map.<String, Object>of(
                            "id", String.valueOf(m.get("id")),
                            "caption", m.get("caption") != null ? m.get("caption") : "",
                            "mediaType", m.get("media_type") != null ? m.get("media_type") : "",
                            "thumbnail", thumb != null ? thumb : "",
                            "permalink", m.get("permalink") != null ? m.get("permalink") : "");
                })
                .toList();
    }

    /** (Re)subscribe an already-connected account to comment/message webhooks. */
    @PostMapping("/{id}/subscribe")
    public Map<String, Object> subscribe(@PathVariable Long id) {
        InstagramAccount acc = repo.findById(id)
                .filter(a -> a.getUserId().equals(currentUser.userId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Instagram account not found"));
        instagram.subscribeToWebhooks(acc.getPageAccessToken(), acc.getIgUserId());
        return Map.of("subscribed", true);
    }

    @DeleteMapping("/{id}")
    public void disconnect(@PathVariable Long id) {
        repo.findById(id)
                .filter(a -> a.getUserId().equals(currentUser.userId()))
                .ifPresent(repo::delete);
    }
}
