package com.dmme.web;

import com.dmme.domain.InstagramAccount;
import com.dmme.repository.InstagramAccountRepository;
import com.dmme.service.CurrentUserService;
import com.dmme.service.InstagramClient;
import com.dmme.service.InstagramOAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

/**
 * One-click "Login with Instagram" OAuth flow.
 *
 * The browser is sent to the authorize URL, Instagram redirects back to the
 * frontend callback with a {@code code}, and the frontend posts that code here
 * (authenticated) to complete the connection server-side.
 */
@RestController
@RequestMapping("/api/instagram/oauth")
public class InstagramOAuthController {

    private static final Logger log = LoggerFactory.getLogger(InstagramOAuthController.class);

    private final InstagramOAuthService oauth;
    private final InstagramAccountRepository repo;
    private final CurrentUserService currentUser;
    private final InstagramClient instagram;

    public InstagramOAuthController(InstagramOAuthService oauth,
                                    InstagramAccountRepository repo,
                                    CurrentUserService currentUser,
                                    InstagramClient instagram) {
        this.oauth = oauth;
        this.repo = repo;
        this.currentUser = currentUser;
        this.instagram = instagram;
    }

    /** Returns the Instagram authorize URL for the frontend to redirect to. */
    @GetMapping("/url")
    public Map<String, Object> url() {
        currentUser.require();
        if (!oauth.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Instagram login is not configured. Set INSTAGRAM_APP_ID, INSTAGRAM_APP_SECRET and INSTAGRAM_REDIRECT_URI.");
        }
        String state = UUID.randomUUID().toString();
        return Map.of("url", oauth.authorizeUrl(state), "state", state);
    }

    public record CallbackRequest(String code) {}

    /** Completes the OAuth flow: exchanges the code and stores the connected account. */
    @PostMapping("/callback")
    public Map<String, Object> callback(@RequestBody CallbackRequest req) {
        var user = currentUser.require();
        if (req == null || req.code() == null || req.code().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing authorization code");
        }

        InstagramOAuthService.ConnectedAccount connected;
        try {
            connected = oauth.exchangeCode(req.code());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to connect Instagram: " + e.getMessage());
        }

        InstagramAccount acc = repo.findByIgUserId(connected.igUserId()).orElseGet(InstagramAccount::new);
        acc.setUserId(user.getId());
        acc.setIgUserId(connected.igUserId());
        acc.setIgUsername(connected.igUsername());
        acc.setPageAccessToken(connected.accessToken());
        acc.setTokenExpiresAt(connected.expiresAt());
        acc.setActive(true);
        repo.save(acc);

        // Subscribe the account to comment/message webhooks so automations fire.
        try {
            instagram.subscribeToWebhooks(acc.getPageAccessToken(), acc.getIgUserId());
        } catch (Exception e) {
            log.warn("Connected {} but failed to subscribe to webhooks: {}",
                    acc.getIgUserId(), e.getMessage());
        }

        return Map.of(
                "id", acc.getId(),
                "igUsername", acc.getIgUsername() != null ? acc.getIgUsername() : "",
                "igUserId", acc.getIgUserId());
    }
}
