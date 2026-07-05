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
        if (acc == null || fromIgsid == null) return;

        String type = (triggerType != null) ? triggerType : "COMMENT";
        logInbound(acc.getUserId(), null, fromIgsid, type, text);

        List<Automation> candidates =
                automations.findByIgAccountIdAndTypeAndStatus(acc.getId(), type, "ACTIVE");

        for (Automation a : candidates) {
            if (a.getIgMediaId() != null && !a.getIgMediaId().equals(mediaId)) continue;
            if (!matches(a, text)) continue;

            if (a.getPublicReply() != null && !a.getPublicReply().isBlank() && commentId != null) {
                safeReply(acc, commentId, a.getPublicReply());
            }
            startFlow(acc, a, fromIgsid, fromUsername);
            break; // one automation per comment
        }
    }

    // ---------------------------------------------------------------------
    // Inbound: direct message
    // ---------------------------------------------------------------------
    @Transactional
    public void handleMessage(String igUserId, String fromIgsid, String fromUsername,
                              String text, boolean storyReply) {
        InstagramAccount acc = igAccounts.findByIgUserId(igUserId).orElse(null);
        if (acc == null || fromIgsid == null) return;

        String channel = storyReply ? "STORY_REPLY" : "DM";
        logInbound(acc.getUserId(), null, fromIgsid, channel, text);

        // 1. Are we mid-flow, waiting on an answer to a question?
        Optional<ConversationState> pending = findPendingState(acc.getId(), fromIgsid);
        if (pending.isPresent()) {
            captureAnswerAndAdvance(acc, pending.get(), fromIgsid, fromUsername, text);
            return;
        }

        // 2. Otherwise match a DM automation, or a STORY_REPLY one for story replies.
        for (Automation a : automations.findByIgAccountIdAndTypeAndStatus(acc.getId(), channel, "ACTIVE")) {
            if (matches(a, text)) {
                startFlow(acc, a, fromIgsid, fromUsername);
                break;
            }
        }
    }

    // ---------------------------------------------------------------------
    // Flow execution
    // ---------------------------------------------------------------------
    private void startFlow(InstagramAccount acc, Automation a, String igsid, String username) {
        upsertLead(acc.getUserId(), a.getId(), igsid, username);

        // Ask-for-follow gate.
        if (a.isAskFollowEnabled()) {
            boolean follows = instagram.isFollower(acc.getPageAccessToken(), acc.getIgUserId(), igsid);
            if (!follows) {
                send(acc, a, igsid, a.getAskFollowMessage() != null
                        ? a.getAskFollowMessage()
                        : "Please follow us first, then reply here to unlock your link!");
                return;
            }
        }
        runFromStep(acc, a, igsid, username, 0);
    }

    private void runFromStep(InstagramAccount acc, Automation a, String igsid, String username, int fromOrder) {
        List<FlowStep> flow = steps.findByAutomationIdOrderByStepOrderAsc(a.getId());
        for (FlowStep step : flow) {
            if (step.getStepOrder() < fromOrder) continue;

            switch (step.getStepType()) {
                case "BUTTONS" -> sendButtons(acc, a, igsid, step);
                case "QUESTION" -> {
                    // Send the question, then pause and wait for a reply.
                    send(acc, a, igsid, step.getBody());
                    saveState(acc.getId(), igsid, a.getId(), step.getStepOrder(),
                            questionField(step));
                    return;
                }
                default -> send(acc, a, igsid, step.getBody());
            }
        }
        // Flow finished -> clear any lingering state.
        clearState(acc.getId(), igsid, a.getId());
    }

    private void captureAnswerAndAdvance(InstagramAccount acc, ConversationState state,
                                         String igsid, String username, String answer) {
        Automation a = automations.findById(state.getAutomationId()).orElse(null);
        if (a == null) { states.delete(state); return; }

        // Store the answer against the lead.
        FlowStep asked = steps.findByAutomationIdOrderByStepOrderAsc(a.getId()).stream()
                .filter(s -> s.getStepOrder() == state.getCurrentStep())
                .findFirst().orElse(null);
        if (asked != null) {
            storeAnswer(acc.getUserId(), a.getId(), igsid, asked, answer);
        }

        // Continue from the next step.
        runFromStep(acc, a, igsid, username, state.getCurrentStep() + 1);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------
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
        if (text == null || text.isBlank()) return;
        AppUser user = users.findById(acc.getUserId()).orElse(null);
        if (user != null && !usage.canSend(user)) {
            log.warn("DM quota exceeded for user {} — skipping send", acc.getUserId());
            logOutbound(acc.getUserId(), a.getId(), igsid, text, "FAILED");
            return;
        }
        try {
            instagram.sendText(acc.getPageAccessToken(), igsid, text);
            usage.recordSend(acc.getUserId());
            logOutbound(acc.getUserId(), a.getId(), igsid, text, "SENT");
        } catch (Exception e) {
            log.error("Failed to send DM", e);
            logOutbound(acc.getUserId(), a.getId(), igsid, text, "FAILED");
        }
    }

    private void sendButtons(InstagramAccount acc, Automation a, String igsid, FlowStep step) {
        AppUser user = users.findById(acc.getUserId()).orElse(null);
        if (user != null && !usage.canSend(user)) return;
        try {
            instagram.sendButtons(acc.getPageAccessToken(), igsid,
                    step.getBody() != null ? step.getBody() : "Tap below:", step.getButtons());
            usage.recordSend(acc.getUserId());
            logOutbound(acc.getUserId(), a.getId(), igsid, step.getBody(), "SENT");
        } catch (Exception e) {
            log.error("Failed to send buttons", e);
        }
    }

    private void safeReply(InstagramAccount acc, String commentId, String message) {
        try {
            instagram.replyToComment(acc.getPageAccessToken(), commentId, message);
        } catch (Exception e) {
            log.error("Failed to reply to comment", e);
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
        // Find any active automation (of any trigger type) with a waiting state
        // for this follower — the follow-up answer always arrives as a DM.
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
