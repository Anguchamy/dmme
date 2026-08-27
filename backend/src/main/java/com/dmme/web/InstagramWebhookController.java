package com.dmme.web;

import com.dmme.config.AppProperties;
import com.dmme.service.AutomationEngine;
import com.dmme.service.InstagramWebhookDiagnostics;
import com.dmme.service.InstagramWebhookDiagnostics.ConfiguredSecret;
import com.dmme.service.InstagramWebhookVerifier;
import com.dmme.service.InstagramWebhookVerifier.VerificationResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Meta webhook endpoint. Configure this URL in the Meta App dashboard:
 *   GET  /api/webhooks/instagram  -> subscription verification (hub.challenge)
 *   POST /api/webhooks/instagram  -> comment / message events
 */
@RestController
@RequestMapping("/api/webhooks/instagram")
public class InstagramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(InstagramWebhookController.class);

    private final AppProperties props;
    private final AutomationEngine engine;
    private final InstagramWebhookVerifier verifier;
    private final ObjectMapper objectMapper;

    public InstagramWebhookController(
            AppProperties props,
            AutomationEngine engine,
            InstagramWebhookVerifier verifier,
            ObjectMapper objectMapper) {
        this.props = props;
        this.engine = engine;
        this.verifier = verifier;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void logConfiguredWebhookSecretsAtStartup() {
        List<ConfiguredSecret> configured = configuredWebhookSecrets();
        if (configured.isEmpty()) {
            log.info("Instagram webhook secrets: none configured (signature verification disabled)");
        } else {
            log.info("Instagram webhook secrets configured: {}",
                    InstagramWebhookDiagnostics.formatStartupSummary(configured));
        }
    }

    /** Meta verification handshake. */
    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if ("subscribe".equals(mode) && props.getInstagram().getVerifyToken().equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).body("Verification failed");
    }

    /** Event delivery. Always return 200 quickly so Meta does not retry. */
    @PostMapping
    public ResponseEntity<String> receive(
            @RequestBody byte[] rawBody,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(name = "User-Agent", required = false) String userAgent) {
        MDC.put("correlationId", newCorrelationId());
        try {
            List<ConfiguredSecret> configuredSecrets = configuredWebhookSecrets();
            if (configuredSecrets.isEmpty()) {
                log.warn("Instagram webhook signature verification disabled: no app secret configured");
            } else {
                List<String> candidateSecrets = configuredSecrets.stream()
                        .map(ConfiguredSecret::value)
                        .toList();
                VerificationResult result = verifier.verifyAny(rawBody, signature, candidateSecrets);
                if (result instanceof VerificationResult.Success success) {
                    log.debug("Instagram webhook signature verified using {} (candidate index {})",
                            configuredSecrets.get(success.matchedSecretIndex()).label(),
                            success.matchedSecretIndex());
                } else {
                    logVerificationFailure(result, configuredSecrets, userAgent);
                    return ResponseEntity.status(403).body("Invalid signature");
                }
            }

            Map<String, Object> payload;
            try {
                payload = objectMapper.readValue(rawBody, new TypeReference<>() {});
            } catch (Exception e) {
                log.error("Error parsing Instagram webhook JSON", e);
                return ResponseEntity.ok("EVENT_RECEIVED");
            }

            log.debug("Instagram webhook payload: {}",
                    truncateForLog(new String(rawBody, StandardCharsets.UTF_8), 2000));

            try {
                dispatch(payload);
            } catch (Exception e) {
                log.error("Error processing Instagram webhook", e);
            }
            return ResponseEntity.ok("EVENT_RECEIVED");
        } finally {
            MDC.remove("correlationId");
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatch(Map<String, Object> payload) {
        List<Map<String, Object>> entries =
                (List<Map<String, Object>>) payload.getOrDefault("entry", List.of());
        log.info("Instagram webhook received: object={} entries={}",
                payload.get("object"), entries.size());
        if (entries.isEmpty()) {
            log.warn("Instagram webhook carries no entries — nothing to dispatch (payload keys: {})",
                    payload.keySet());
            return;
        }

        for (Map<String, Object> entry : entries) {
            String igUserId = String.valueOf(entry.get("id"));

            // --- Direct messages (Messenger platform shape) ---
            List<Map<String, Object>> messaging =
                    (List<Map<String, Object>>) entry.get("messaging");
            // --- Comments / mentions (changes shape) ---
            List<Map<String, Object>> changes =
                    (List<Map<String, Object>>) entry.get("changes");

            if (messaging == null && changes == null) {
                log.warn("Instagram webhook entry for igUser={} has neither messaging nor changes"
                        + " — ignoring (entry keys: {})", igUserId, entry.keySet());
                continue;
            }

            if (messaging != null) {
                for (Map<String, Object> m : messaging) {
                    handleMessaging(igUserId, m);
                }
            }
            if (changes != null) {
                for (Map<String, Object> change : changes) {
                    handleChange(igUserId, change);
                }
            }
        }
    }

    private List<ConfiguredSecret> configuredWebhookSecrets() {
        List<ConfiguredSecret> secrets = new ArrayList<>(2);
        boolean webhookSecretConfigured = false;
        String webhookSecret = props.getInstagram().getWebhookAppSecret();
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            secrets.add(new ConfiguredSecret(0, "webhook app secret", webhookSecret));
            webhookSecretConfigured = true;
        }
        String appSecret = props.getInstagram().getAppSecret();
        if (appSecret != null && !appSecret.isBlank()) {
            String label = webhookSecretConfigured ? "OAuth app secret (fallback)" : "OAuth app secret";
            secrets.add(new ConfiguredSecret(secrets.size(), label, appSecret));
        }
        return secrets;
    }

    private void logVerificationFailure(
            VerificationResult result, List<ConfiguredSecret> configuredSecrets, String userAgent) {
        switch (result) {
            case VerificationResult.MissingHeader ignored -> {
                if (userAgent != null && !userAgent.isBlank()) {
                    log.debug("Instagram webhook rejected: no X-Hub-Signature-256 header (User-Agent: {})", userAgent);
                } else {
                    log.debug("Instagram webhook rejected: no X-Hub-Signature-256 header");
                }
            }
            case VerificationResult.MalformedHeader malformed -> log.warn(
                    "Instagram webhook rejected: X-Hub-Signature-256 header present but malformed ({})",
                    describeMalformation(malformed.reason()));
            case VerificationResult.NoMatch noMatch -> log.warn(
                    InstagramWebhookDiagnostics.formatNoMatchMessage(noMatch, configuredSecrets));
            default -> log.warn("Instagram webhook rejected: signature verification failed");
        }
    }

    private static String describeMalformation(VerificationResult.MalformedHeader.Malformation reason) {
        return switch (reason) {
            case MISSING_PREFIX -> "missing sha256= prefix";
            case EMPTY_DIGEST -> "empty digest after sha256= prefix";
            case INVALID_HEX -> "digest is not valid hexadecimal";
            case UNEXPECTED_DIGEST_LENGTH -> "digest length is not 32 bytes";
        };
    }

    @SuppressWarnings("unchecked")
    private void handleMessaging(String igUserId, Map<String, Object> m) {
        Map<String, Object> sender = (Map<String, Object>) m.get("sender");
        Map<String, Object> message = (Map<String, Object>) m.get("message");
        if (sender == null || message == null) {
            String kind = messagingEventKind(m);
            if (kind != null) {
                log.info("Ignoring Instagram {} event for igUser={} — automations only run on"
                        + " inbound messages", kind, igUserId);
            } else {
                log.info("Ignoring messaging event for igUser={} with no sender or message (keys: {})",
                        igUserId, m.keySet());
            }
            return;
        }
        if (Boolean.TRUE.equals(message.get("is_echo"))) {
            log.debug("Ignoring echo of our own outbound message for igUser={}", igUserId);
            return;
        }

        String fromIgsid = String.valueOf(sender.get("id"));
        String text = message.get("text") != null ? String.valueOf(message.get("text")) : null;

        Map<String, Object> replyTo = (Map<String, Object>) message.get("reply_to");
        boolean storyReply = replyTo != null && replyTo.get("story") != null;

        log.info("Message webhook: igUser={} from={} storyReply={} text={}",
                igUserId, fromIgsid, storyReply, truncateForLog(text));
        if (text == null) {
            log.info("Message from={} carries no text (attachment, reaction or share),"
                    + " so keyword automations cannot match it", fromIgsid);
        }
        engine.handleMessage(igUserId, fromIgsid, null, text, storyReply);
    }

    @SuppressWarnings("unchecked")
    private void handleChange(String igUserId, Map<String, Object> change) {
        String field = String.valueOf(change.get("field"));
        String triggerType = switch (field) {
            case "comments" -> "COMMENT";
            case "live_comments" -> "LIVE";
            default -> null;
        };
        if (triggerType == null) {
            log.warn("Ignoring unhandled Instagram webhook field={} for igUser={}"
                    + " — only comments and live_comments drive automations", field, igUserId);
            return;
        }

        Map<String, Object> value = (Map<String, Object>) change.get("value");
        if (value == null) {
            log.warn("Instagram webhook field={} for igUser={} has no value object — ignoring",
                    field, igUserId);
            return;
        }

        String commentId = String.valueOf(value.get("id"));
        String text = value.get("text") != null ? String.valueOf(value.get("text")) : null;

        Map<String, Object> from = (Map<String, Object>) value.get("from");
        String fromIgsid = from != null ? String.valueOf(from.get("id")) : null;
        String fromUsername = from != null && from.get("username") != null
                ? String.valueOf(from.get("username")) : null;

        Map<String, Object> media = (Map<String, Object>) value.get("media");
        String mediaId = media != null ? String.valueOf(media.get("id")) : null;

        log.info("{} webhook: igUser={} media={} from=@{} text={}",
                triggerType, igUserId, mediaId, fromUsername, truncateForLog(text));
        engine.handleComment(igUserId, mediaId, commentId, fromIgsid, fromUsername, text, triggerType);
    }

    /**
     * Name the messaging shapes Meta delivers that are not inbound messages, so they
     * are not mistaken for malformed payloads. Meta sends these on the "messages"
     * subscription alongside (or instead of) the message itself.
     */
    private static String messagingEventKind(Map<String, Object> m) {
        for (String key : List.of("message_edit", "reaction", "read", "delivery",
                "postback", "referral", "seen", "message_reactions")) {
            if (m.containsKey(key)) return key;
        }
        return null;
    }

    private static String newCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String truncateForLog(String text) {
        return truncateForLog(text, 80);
    }

    private static String truncateForLog(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
