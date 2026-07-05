package com.dmme.web;

import com.dmme.config.AppProperties;
import com.dmme.service.AutomationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    public InstagramWebhookController(AppProperties props, AutomationEngine engine) {
        this.props = props;
        this.engine = engine;
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
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> receive(@RequestBody Map<String, Object> payload) {
        try {
            List<Map<String, Object>> entries =
                    (List<Map<String, Object>>) payload.getOrDefault("entry", List.of());

            for (Map<String, Object> entry : entries) {
                String igUserId = String.valueOf(entry.get("id"));

                // --- Direct messages (Messenger platform shape) ---
                List<Map<String, Object>> messaging =
                        (List<Map<String, Object>>) entry.get("messaging");
                if (messaging != null) {
                    for (Map<String, Object> m : messaging) {
                        handleMessaging(igUserId, m);
                    }
                }

                // --- Comments / mentions (changes shape) ---
                List<Map<String, Object>> changes =
                        (List<Map<String, Object>>) entry.get("changes");
                if (changes != null) {
                    for (Map<String, Object> change : changes) {
                        handleChange(igUserId, change);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing Instagram webhook", e);
        }
        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    @SuppressWarnings("unchecked")
    private void handleMessaging(String igUserId, Map<String, Object> m) {
        Map<String, Object> sender = (Map<String, Object>) m.get("sender");
        Map<String, Object> message = (Map<String, Object>) m.get("message");
        if (sender == null || message == null) return;
        if (Boolean.TRUE.equals(message.get("is_echo"))) return; // ignore our own outbound

        String fromIgsid = String.valueOf(sender.get("id"));
        String text = message.get("text") != null ? String.valueOf(message.get("text")) : null;

        // A reply to one of our stories arrives as a message with reply_to.story.
        Map<String, Object> replyTo = (Map<String, Object>) message.get("reply_to");
        boolean storyReply = replyTo != null && replyTo.get("story") != null;

        log.info("Message webhook: igUser={} from={} storyReply={} text={}",
                igUserId, fromIgsid, storyReply, text);
        engine.handleMessage(igUserId, fromIgsid, null, text, storyReply);
    }

    @SuppressWarnings("unchecked")
    private void handleChange(String igUserId, Map<String, Object> change) {
        String field = String.valueOf(change.get("field"));
        // Post/reel comments -> COMMENT; live video comments -> LIVE.
        String triggerType = switch (field) {
            case "comments" -> "COMMENT";
            case "live_comments" -> "LIVE";
            default -> null;
        };
        if (triggerType == null) return;

        Map<String, Object> value = (Map<String, Object>) change.get("value");
        if (value == null) return;

        String commentId = String.valueOf(value.get("id"));
        String text = value.get("text") != null ? String.valueOf(value.get("text")) : null;

        Map<String, Object> from = (Map<String, Object>) value.get("from");
        String fromIgsid = from != null ? String.valueOf(from.get("id")) : null;
        String fromUsername = from != null && from.get("username") != null
                ? String.valueOf(from.get("username")) : null;

        Map<String, Object> media = (Map<String, Object>) value.get("media");
        String mediaId = media != null ? String.valueOf(media.get("id")) : null;

        log.info("{} webhook: igUser={} media={} from=@{} text={}",
                triggerType, igUserId, mediaId, fromUsername, text);
        engine.handleComment(igUserId, mediaId, commentId, fromIgsid, fromUsername, text, triggerType);
    }
}
