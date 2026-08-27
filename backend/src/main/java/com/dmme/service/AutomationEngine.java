package com.dmme.service;

import com.dmme.domain.*;
import com.dmme.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core of the product: reacts to inbound Instagram events (comments + DMs),
 * matches them against the user's active automations, runs the DM flow, asks
 * the configured questions, and records leads.
 */
@Service
public class AutomationEngine {

    private static final Logger log = LoggerFactory.getLogger(AutomationEngine.class);
    private static final int LOG_TEXT_MAX = 80;

    private final AutomationRepository automations;
    private final TriggerKeywordRepository keywords;
    private final FlowStepRepository steps;
    private final InstagramAccountRepository igAccounts;
    private final ConversationStateRepository states;
    private final LeadRepository leads;
    private final MessageLogRepository logs;
    private final AppUserRepository users;
    private final InstagramClient instagram;
    private final UsageService usage;

    public AutomationEngine(AutomationRepository automations, TriggerKeywordRepository keywords,
                            FlowStepRepository steps, InstagramAccountRepository igAccounts,
                            ConversationStateRepository states, LeadRepository leads,
                            MessageLogRepository logs, AppUserRepository users,
                            InstagramClient instagram, UsageService usage) {
        this.automations = automations;
        this.keywords = keywords;
        this.steps = steps;
        this.igAccounts = igAccounts;
        this.states = states;
        this.leads = leads;
        this.logs = logs;
        this.users = users;
        this.instagram = instagram;
        this.usage = usage;
    }

    // ---------------------------------------------------------------------
    // Inbound: comment on a post/reel (COMMENT) or on a live video (LIVE)
    // ---------------------------------------------------------------------
    @Transactional
    public void handleComment(String igUserId, String mediaId, String commentId,
                              String fromIgsid, String fromUsername, String text, String triggerType) {
        InstagramAccount acc = igAccounts.findByIgUserId(igUserId).orElse(null);
        if (acc == null) {
            log.warn("No connected Instagram account for igUserId={} — connect the account in Settings",
                    igUserId);
            return;
        }
        if (fromIgsid == null) {
            log.warn("Comment event missing sender id (fromIgsid) for igUserId={}", igUserId);
            return;
        }

        String type = (triggerType != null) ? triggerType : "COMMENT";
        log.info("Handling {} event for igAccountId={} from={} text=\"{}\"",
                type, acc.getId(), fromIgsid, truncate(text));
        logInbound(acc.getUserId(), null, fromIgsid, type, text);

        List<Automation> candidates =
                automations.findByIgAccountIdAndTypeAndStatus(acc.getId(), type, "ACTIVE");
        logCandidateCount(acc.getId(), type, "ACTIVE", candidates.size());

        boolean matched = false;
        for (Automation a : candidates) {
            if (a.getIgMediaId() != null && !a.getIgMediaId().equals(mediaId)) {
                log.info("Skipping automation id={} name=\"{}\": media id mismatch (expected={}, actual={})",
                        a.getId(), a.getName(), a.getIgMediaId(), mediaId);
                continue;
            }
            if (!matches(a, text)) {
                log.info("Skipping automation id={} name=\"{}\": keyword mismatch for text=\"{}\"",
                        a.getId(), a.getName(), truncate(text));
                continue;
            }

            log.info("Selected automation id={} name=\"{}\"", a.getId(), a.getName());
            if (a.getPublicReply() != null && !a.getPublicReply().isBlank() && commentId != null) {
                safeReply(acc, commentId, a.getPublicReply());
            }
            startFlow(acc, a, fromIgsid, fromUsername);
            matched = true;
            break;
        }
        if (!matched && !candidates.isEmpty()) {
            log.info("No automation matched after evaluating {} candidate(s)", candidates.size());
        }
    }

    // ---------------------------------------------------------------------
    // Inbound: direct message
    // ---------------------------------------------------------------------
    @Transactional
    public void handleMessage(String igUserId, String fromIgsid, String fromUsername,
                              String text, boolean storyReply) {
        InstagramAccount acc = igAccounts.findByIgUserId(igUserId).orElse(null);
        if (acc == null) {
            log.warn("No connected Instagram account for igUserId={} — connect the account in Settings",
                    igUserId);
            return;
        }
        if (fromIgsid == null) {
            log.warn("Message event missing sender id (fromIgsid) for igUserId={}", igUserId);
            return;
        }

        String channel = storyReply ? "STORY_REPLY" : "DM";

        Optional<ConversationState> pending = findPendingState(acc.getId(), fromIgsid);
        if (pending.isPresent()) {
            ConversationState state = pending.get();
            log.info("Handling {} reply for igAccountId={} from={} (awaiting field={})",
                    channel, acc.getId(), fromIgsid, state.getAwaitingField());
            logInbound(acc.getUserId(), null, fromIgsid, channel, text);
            log.info("Resuming paused conversation for automation id={} at step order={} awaiting field={}",
                    state.getAutomationId(), state.getCurrentStep(), state.getAwaitingField());
            captureAnswerAndAdvance(acc, state, fromIgsid, fromUsername, text);
            return;
        }

        log.info("Handling {} event for igAccountId={} from={} text=\"{}\"",
                channel, acc.getId(), fromIgsid, truncate(text));
        logInbound(acc.getUserId(), null, fromIgsid, channel, text);

        List<Automation> candidates =
                automations.findByIgAccountIdAndTypeAndStatus(acc.getId(), channel, "ACTIVE");
        logCandidateCount(acc.getId(), channel, "ACTIVE", candidates.size());

        boolean matched = false;
        for (Automation a : candidates) {
            if (!matches(a, text)) {
                log.info("Skipping automation id={} name=\"{}\": keyword mismatch for text=\"{}\"",
                        a.getId(), a.getName(), truncate(text));
                continue;
            }
            log.info("Selected automation id={} name=\"{}\"", a.getId(), a.getName());
            startFlow(acc, a, fromIgsid, fromUsername);
            matched = true;
            break;
        }
        if (!matched && !candidates.isEmpty()) {
            log.info("No automation matched after evaluating {} candidate(s)", candidates.size());
        }
    }

    // ---------------------------------------------------------------------
    // Flow execution
    // ---------------------------------------------------------------------
    private void startFlow(InstagramAccount acc, Automation a, String igsid, String username) {
        upsertLead(acc.getUserId(), a.getId(), igsid, username);

        if (a.isAskFollowEnabled()) {
            boolean follows = instagram.isFollower(acc.getPageAccessToken(), acc.getIgUserId(), igsid);
            log.info("Follow-gate check for automation id={}: sender follows={}", a.getId(), follows);
            if (!follows) {
                log.info("Sender does not follow — sending follow-gate message instead of flow for automation id={}",
                        a.getId());
                send(acc, a, igsid, a.getAskFollowMessage() != null
                        ? a.getAskFollowMessage()
                        : "Please follow us first, then reply here to unlock your link!");
                return;
            }
        }
        log.info("Starting flow for automation id={} name=\"{}\"", a.getId(), a.getName());
        runFromStep(acc, a, igsid, username, 0);
    }

    private void runFromStep(InstagramAccount acc, Automation a, String igsid, String username, int fromOrder) {
        List<FlowStep> flow = steps.findByAutomationIdOrderByStepOrderAsc(a.getId());
        log.info("Running flow for automation id={} from step order={}, {} step(s) total",
                a.getId(), fromOrder, flow.size());
        if (flow.isEmpty()) {
            log.warn("Automation id={} has an empty flow — nothing to send", a.getId());
            clearState(acc.getId(), igsid, a.getId());
            return;
        }

        for (FlowStep step : flow) {
            if (step.getStepOrder() < fromOrder) continue;

            log.info("Executing step order={} type={} for automation id={}",
                    step.getStepOrder(), step.getStepType(), a.getId());

            switch (step.getStepType()) {
                case "BUTTONS" -> sendButtons(acc, a, igsid, step);
                case "QUESTION" -> {
                    send(acc, a, igsid, step.getBody());
                    String awaiting = questionField(step);
                    saveState(acc.getId(), igsid, a.getId(), step.getStepOrder(), awaiting);
                    log.info("Pausing flow at step order={} awaiting field={} for automation id={}",
                            step.getStepOrder(), awaiting, a.getId());
                    return;
                }
                default -> send(acc, a, igsid, step.getBody());
            }
        }
        log.info("Flow completed for automation id={}", a.getId());
        clearState(acc.getId(), igsid, a.getId());
    }

    private void captureAnswerAndAdvance(InstagramAccount acc, ConversationState state,
                                         String igsid, String username, String answer) {
        Automation a = automations.findById(state.getAutomationId()).orElse(null);
        if (a == null) {
            log.warn("Automation id={} no longer exists — discarding conversation state",
                    state.getAutomationId());
            states.delete(state);
            return;
        }

        FlowStep asked = steps.findByAutomationIdOrderByStepOrderAsc(a.getId()).stream()
                .filter(s -> s.getStepOrder() == state.getCurrentStep())
                .findFirst().orElse(null);
        if (asked == null) {
            log.warn("Could not find step order={} for automation id={} — answer not stored",
                    state.getCurrentStep(), a.getId());
        } else {
            storeAnswer(acc.getUserId(), a.getId(), igsid, asked, answer);
            log.info("Stored answer for field={} (length={}) for automation id={}",
                    questionField(asked), answer != null ? answer.length() : 0, a.getId());
        }

        runFromStep(acc, a, igsid, username, state.getCurrentStep() + 1);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------
    private void logCandidateCount(Long igAccountId, String type, String status, int count) {
        log.info("Found {} candidate automation(s) for igAccountId={} type={} status={}",
                count, igAccountId, type, status);
        if (count == 0) {
            long total = automations.countByIgAccountId(igAccountId);
            log.info("No matching automations — account has {} automation(s) in any type/status", total);
        }
    }

    private static String truncate(String text) {
        if (text == null) return "";
        if (text.length() <= LOG_TEXT_MAX) return text;
        return text.substring(0, LOG_TEXT_MAX) + "...";
    }

    private boolean matches(Automation a, String text) {
        if (a.isMatchAny()) return true;
        if (text == null) return false;
        String lower = text.toLowerCase();
        return keywords.findByAutomationId(a.getId()).stream().anyMatch(k -> {
            String kw = k.getKeyword().toLowerCase().trim();
            return "EXACT".equalsIgnoreCase(k.getMatchType())
                    ? lower.trim().equals(kw)
                    : lower.contains(kw);
        });
    }

    private void send(InstagramAccount acc, Automation a, String igsid, String text) {
        if (text == null || text.isBlank()) {
            log.info("Skipping blank DM body for automation id={} recipient={}", a.getId(), igsid);
            return;
        }
        AppUser user = users.findById(acc.getUserId()).orElse(null);
        if (user != null && !usage.canSend(user)) {
            log.warn("DM quota exceeded for user {} — skipping send (automation id={}, recipient={})",
                    acc.getUserId(), a.getId(), igsid);
            logOutbound(acc.getUserId(), a.getId(), igsid, text, "FAILED");
            return;
        }
        try {
            instagram.sendText(acc.getPageAccessToken(), igsid, text);
            usage.recordSend(acc.getUserId());
            logOutbound(acc.getUserId(), a.getId(), igsid, text, "SENT");
            log.info("Sent DM to recipient={} automation id={} body=\"{}\"",
                    igsid, a.getId(), truncate(text));
        } catch (Exception e) {
            log.error("Failed to send DM to recipient={} automation id={}", igsid, a.getId(), e);
            logOutbound(acc.getUserId(), a.getId(), igsid, text, "FAILED");
        }
    }

    private void sendButtons(InstagramAccount acc, Automation a, String igsid, FlowStep step) {
        String body = step.getBody();
        if (body == null || body.isBlank()) {
            log.info("Skipping blank button body for automation id={} recipient={}", a.getId(), igsid);
            return;
        }
        AppUser user = users.findById(acc.getUserId()).orElse(null);
        if (user != null && !usage.canSend(user)) {
            log.warn("DM quota exceeded for user {} — skipping button send (automation id={}, recipient={})",
                    acc.getUserId(), a.getId(), igsid);
            return;
        }
        try {
            instagram.sendButtons(acc.getPageAccessToken(), igsid, body, step.getButtons());
            usage.recordSend(acc.getUserId());
            logOutbound(acc.getUserId(), a.getId(), igsid, body, "SENT");
            log.info("Sent button DM to recipient={} automation id={} body=\"{}\"",
                    igsid, a.getId(), truncate(body));
        } catch (Exception e) {
            log.error("Failed to send buttons to recipient={} automation id={}", igsid, a.getId(), e);
        }
    }

    private void safeReply(InstagramAccount acc, String commentId, String message) {
        try {
            instagram.replyToComment(acc.getPageAccessToken(), commentId, message);
        } catch (Exception e) {
            log.error("Failed to reply to comment {}", commentId, e);
        }
    }

    private String questionField(FlowStep step) {
        if ("CUSTOM".equalsIgnoreCase(step.getCollectField()) && step.getCustomKey() != null) {
            return "CUSTOM:" + step.getCustomKey();
        }
        return step.getCollectField();
    }

    private void storeAnswer(UUID userId, Long automationId, String igsid, FlowStep step, String answer) {
        Lead lead = leads.findByUserIdAndAutomationIdAndIgUserId(userId, automationId, igsid)
                .orElseGet(() -> {
                    Lead l = new Lead();
                    l.setUserId(userId);
                    l.setAutomationId(automationId);
                    l.setIgUserId(igsid);
                    return l;
                });
        String field = step.getCollectField();
        if ("EMAIL".equalsIgnoreCase(field)) lead.setEmail(answer);
        else if ("PHONE".equalsIgnoreCase(field)) lead.setPhone(answer);
        else if ("NAME".equalsIgnoreCase(field)) lead.setName(answer);
        else if ("CUSTOM".equalsIgnoreCase(field) && step.getCustomKey() != null)
            lead.getCustomData().put(step.getCustomKey(), answer);
        leads.save(lead);
    }

    private void upsertLead(UUID userId, Long automationId, String igsid, String username) {
        Lead lead = leads.findByUserIdAndAutomationIdAndIgUserId(userId, automationId, igsid)
                .orElseGet(() -> {
                    Lead l = new Lead();
                    l.setUserId(userId);
                    l.setAutomationId(automationId);
                    l.setIgUserId(igsid);
                    return l;
                });
        if (username != null) lead.setIgUsername(username);
        leads.save(lead);
    }

    private Optional<ConversationState> findPendingState(Long igAccountId, String igsid) {
        for (Automation a : automations.findByIgAccountIdAndStatus(igAccountId, "ACTIVE")) {
            var s = states.findByIgAccountIdAndIgUserIdAndAutomationId(igAccountId, igsid, a.getId());
            if (s.isPresent() && s.get().getAwaitingField() != null) return s;
        }
        return Optional.empty();
    }

    private void saveState(Long igAccountId, String igsid, Long automationId, int step, String awaitingField) {
        ConversationState s = states
                .findByIgAccountIdAndIgUserIdAndAutomationId(igAccountId, igsid, automationId)
                .orElseGet(ConversationState::new);
        s.setIgAccountId(igAccountId);
        s.setIgUserId(igsid);
        s.setAutomationId(automationId);
        s.setCurrentStep(step);
        s.setAwaitingField(awaitingField);
        states.save(s);
    }

    private void clearState(Long igAccountId, String igsid, Long automationId) {
        states.findByIgAccountIdAndIgUserIdAndAutomationId(igAccountId, igsid, automationId)
                .ifPresent(states::delete);
    }

    private void logInbound(UUID userId, Long automationId, String igsid, String channel, String content) {
        MessageLog m = new MessageLog();
        m.setUserId(userId);
        m.setAutomationId(automationId);
        m.setIgUserId(igsid);
        m.setDirection("IN");
        m.setChannel(channel);
        m.setContent(content);
        m.setStatus("RECEIVED");
        logs.save(m);
    }

    private void logOutbound(UUID userId, Long automationId, String igsid, String content, String status) {
        MessageLog m = new MessageLog();
        m.setUserId(userId);
        m.setAutomationId(automationId);
        m.setIgUserId(igsid);
        m.setDirection("OUT");
        m.setChannel("DM");
        m.setContent(content);
        m.setStatus(status);
        logs.save(m);
    }
}
