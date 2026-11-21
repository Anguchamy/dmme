package com.dmme.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dmme.domain.*;
import com.dmme.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutomationEngineTest {

    private static final String IG_USER_ID = "17841448037418309";
    private static final String FROM_IGSID = "sender-igsid-123";
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String ACCESS_TOKEN = "page-token-do-not-log";

    @Mock private AutomationRepository automations;
    @Mock private TriggerKeywordRepository keywords;
    @Mock private FlowStepRepository steps;
    @Mock private InstagramAccountRepository igAccounts;
    @Mock private ConversationStateRepository states;
    @Mock private LeadRepository leads;
    @Mock private MessageLogRepository logs;
    @Mock private AppUserRepository users;
    @Mock private InstagramClient instagram;
    @Mock private UsageService usage;

    private AutomationEngine engine;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        engine = new AutomationEngine(
                automations, keywords, steps, igAccounts, states, leads, logs, users, instagram, usage);

        Logger logger = (Logger) LoggerFactory.getLogger(AutomationEngine.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(AutomationEngine.class);
        logger.detachAppender(logAppender);
    }

    @Test
    void unknownIgUserIdLogsNotConnectedWarning() {
        when(igAccounts.findByIgUserId(IG_USER_ID)).thenReturn(Optional.empty());

        engine.handleMessage(IG_USER_ID, FROM_IGSID, null, "hello", false);

        String warn = findLog(Level.WARN);
        assertTrue(warn.contains("No connected Instagram account for igUserId=" + IG_USER_ID));
        assertTrue(warn.contains("connect the account in Settings"));
        verifyNoInteractions(automations);
    }

    @Test
    void zeroMatchingAutomationsLogsCounts() {
        InstagramAccount acc = connectedAccount();
        when(igAccounts.findByIgUserId(IG_USER_ID)).thenReturn(Optional.of(acc));
        when(automations.findByIgAccountIdAndTypeAndStatus(acc.getId(), "DM", "ACTIVE")).thenReturn(List.of());
        when(automations.countByIgAccountId(acc.getId())).thenReturn(2L);
        when(automations.findByIgAccountIdAndStatus(acc.getId(), "ACTIVE")).thenReturn(List.of());
        when(logs.save(any(MessageLog.class))).thenAnswer(inv -> inv.getArgument(0));

        engine.handleMessage(IG_USER_ID, FROM_IGSID, null, "hello", false);

        assertTrue(hasInfo("Found 0 candidate automation(s) for igAccountId=" + acc.getId() + " type=DM status=ACTIVE"));
        assertTrue(hasInfo("No matching automations — account has 2 automation(s) in any type/status"));
    }

    @Test
    void keywordMismatchLogsSkipReasonWithAutomationId() {
        InstagramAccount acc = connectedAccount();
        Automation automation = dmAutomation(42L, "Welcome DM");
        automation.setMatchAny(false);

        when(igAccounts.findByIgUserId(IG_USER_ID)).thenReturn(Optional.of(acc));
        when(automations.findByIgAccountIdAndTypeAndStatus(acc.getId(), "DM", "ACTIVE"))
                .thenReturn(List.of(automation));
        when(keywords.findByAutomationId(42L)).thenReturn(List.of(keyword("price", "CONTAINS")));
        when(states.findByIgAccountIdAndIgUserIdAndAutomationId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(automations.findByIgAccountIdAndStatus(acc.getId(), "ACTIVE")).thenReturn(List.of(automation));
        when(logs.save(any(MessageLog.class))).thenAnswer(inv -> inv.getArgument(0));

        engine.handleMessage(IG_USER_ID, FROM_IGSID, null, "hello", false);

        assertTrue(hasInfo("Skipping automation id=42 name=\"Welcome DM\": keyword mismatch"));
        assertTrue(hasInfo("No automation matched after evaluating 1 candidate(s)"));
    }

    @Test
    void emptyFlowWarns() {
        InstagramAccount acc = connectedAccount();
        Automation automation = dmAutomation(7L, "Empty flow");
        automation.setMatchAny(true);

        when(igAccounts.findByIgUserId(IG_USER_ID)).thenReturn(Optional.of(acc));
        when(automations.findByIgAccountIdAndTypeAndStatus(acc.getId(), "DM", "ACTIVE"))
                .thenReturn(List.of(automation));
        when(states.findByIgAccountIdAndIgUserIdAndAutomationId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(automations.findByIgAccountIdAndStatus(acc.getId(), "ACTIVE")).thenReturn(List.of(automation));
        when(logs.save(any(MessageLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(leads.findByUserIdAndAutomationIdAndIgUserId(USER_ID, 7L, FROM_IGSID)).thenReturn(Optional.empty());
        when(leads.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));
        when(steps.findByAutomationIdOrderByStepOrderAsc(7L)).thenReturn(List.of());

        engine.handleMessage(IG_USER_ID, FROM_IGSID, null, "go", false);

        assertTrue(hasWarn("Automation id=7 has an empty flow — nothing to send"));
    }

    @Test
    void questionStepLogsPauseAndAwaitedField() {
        InstagramAccount acc = connectedAccount();
        Automation automation = dmAutomation(9L, "Lead capture");
        automation.setMatchAny(true);

        FlowStep question = new FlowStep();
        question.setAutomationId(9L);
        question.setStepOrder(0);
        question.setStepType("QUESTION");
        question.setBody("What's your email?");
        question.setCollectField("EMAIL");

        AppUser user = new AppUser();
        user.setId(USER_ID);

        when(igAccounts.findByIgUserId(IG_USER_ID)).thenReturn(Optional.of(acc));
        when(automations.findByIgAccountIdAndTypeAndStatus(acc.getId(), "DM", "ACTIVE"))
                .thenReturn(List.of(automation));
        when(states.findByIgAccountIdAndIgUserIdAndAutomationId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(automations.findByIgAccountIdAndStatus(acc.getId(), "ACTIVE")).thenReturn(List.of(automation));
        when(logs.save(any(MessageLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(leads.findByUserIdAndAutomationIdAndIgUserId(USER_ID, 9L, FROM_IGSID)).thenReturn(Optional.empty());
        when(leads.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));
        when(steps.findByAutomationIdOrderByStepOrderAsc(9L)).thenReturn(List.of(question));
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(usage.canSend(user)).thenReturn(true);
        when(states.findByIgAccountIdAndIgUserIdAndAutomationId(acc.getId(), FROM_IGSID, 9L))
                .thenReturn(Optional.empty());
        when(states.save(any(ConversationState.class))).thenAnswer(inv -> inv.getArgument(0));

        engine.handleMessage(IG_USER_ID, FROM_IGSID, null, "go", false);

        assertTrue(hasInfo("Executing step order=0 type=QUESTION for automation id=9"));
        assertTrue(hasInfo("Pausing flow at step order=0 awaiting field=EMAIL for automation id=9"));
    }

    @Test
    void successfulSendLogsRecipientAndTruncatedBody() {
        InstagramAccount acc = connectedAccount();
        Automation automation = dmAutomation(3L, "Reply bot");
        automation.setMatchAny(true);

        String longBody = "A".repeat(100);
        FlowStep textStep = new FlowStep();
        textStep.setAutomationId(3L);
        textStep.setStepOrder(0);
        textStep.setStepType("TEXT");
        textStep.setBody(longBody);

        AppUser user = new AppUser();
        user.setId(USER_ID);

        when(igAccounts.findByIgUserId(IG_USER_ID)).thenReturn(Optional.of(acc));
        when(automations.findByIgAccountIdAndTypeAndStatus(acc.getId(), "DM", "ACTIVE"))
                .thenReturn(List.of(automation));
        when(states.findByIgAccountIdAndIgUserIdAndAutomationId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(automations.findByIgAccountIdAndStatus(acc.getId(), "ACTIVE")).thenReturn(List.of(automation));
        when(logs.save(any(MessageLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(leads.findByUserIdAndAutomationIdAndIgUserId(USER_ID, 3L, FROM_IGSID)).thenReturn(Optional.empty());
        when(leads.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));
        when(steps.findByAutomationIdOrderByStepOrderAsc(3L)).thenReturn(List.of(textStep));
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(usage.canSend(user)).thenReturn(true);

        engine.handleMessage(IG_USER_ID, FROM_IGSID, null, "go", false);

        verify(instagram).sendText(ACCESS_TOKEN, FROM_IGSID, longBody);
        assertTrue(hasInfo("Sent DM to recipient=" + FROM_IGSID + " automation id=3 body=\"" + "A".repeat(80) + "...\""));
        assertFalse(allLogs().contains(longBody));
    }

    @Test
    void leadAnswerValueNeverAppearsInLogs() {
        InstagramAccount acc = connectedAccount();
        Automation automation = dmAutomation(11L, "Lead capture");
        String secretAnswer = "user@secret-email.com";

        FlowStep question = new FlowStep();
        question.setAutomationId(11L);
        question.setStepOrder(0);
        question.setStepType("QUESTION");
        question.setBody("Email?");
        question.setCollectField("EMAIL");

        ConversationState state = new ConversationState();
        state.setAutomationId(11L);
        state.setCurrentStep(0);
        state.setAwaitingField("EMAIL");
        state.setIgAccountId(acc.getId());
        state.setIgUserId(FROM_IGSID);

        Lead lead = new Lead();
        lead.setUserId(USER_ID);
        lead.setAutomationId(11L);
        lead.setIgUserId(FROM_IGSID);

        when(igAccounts.findByIgUserId(IG_USER_ID)).thenReturn(Optional.of(acc));
        when(automations.findByIgAccountIdAndStatus(acc.getId(), "ACTIVE")).thenReturn(List.of(automation));
        when(states.findByIgAccountIdAndIgUserIdAndAutomationId(acc.getId(), FROM_IGSID, 11L))
                .thenReturn(Optional.of(state));
        when(automations.findById(11L)).thenReturn(Optional.of(automation));
        when(steps.findByAutomationIdOrderByStepOrderAsc(11L)).thenReturn(List.of(question));
        when(leads.findByUserIdAndAutomationIdAndIgUserId(USER_ID, 11L, FROM_IGSID))
                .thenReturn(Optional.of(lead));
        when(leads.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));
        when(logs.save(any(MessageLog.class))).thenAnswer(inv -> inv.getArgument(0));

        engine.handleMessage(IG_USER_ID, FROM_IGSID, null, secretAnswer, false);

        assertTrue(hasInfo("Stored answer for field=EMAIL (length=" + secretAnswer.length() + ") for automation id=11"));
        assertFalse(allLogs().contains(secretAnswer));
        assertFalse(allLogs().contains("user@secret"));
    }

    @Test
    void selfCommentIsIgnoredAndDoesNotStartFlow() {
        InstagramAccount acc = connectedAccount();
        when(igAccounts.findByIgUserId(IG_USER_ID)).thenReturn(Optional.of(acc));

        engine.handleComment(IG_USER_ID, "media-1", "comment-1", IG_USER_ID, "dm____________me",
                "Thanks! Check your DMs", "COMMENT");

        assertTrue(hasInfo("Ignoring self-comment from connected account igUserId=" + IG_USER_ID));
        verifyNoInteractions(automations);
        verify(instagram, never()).sendText(any(), any(), any());
        verify(instagram, never()).replyToComment(any(), any(), any());
    }

    @Test
    void commentMatchSendsPrivateReplyViaCommentId() {
        InstagramAccount acc = connectedAccount();
        Automation automation = new Automation();
        automation.setId(11L);
        automation.setUserId(USER_ID);
        automation.setIgAccountId(1L);
        automation.setName("App Review comment test");
        automation.setType("COMMENT");
        automation.setStatus("ACTIVE");
        automation.setMatchAny(true);
        automation.setIgMediaId("media-1");

        FlowStep step = new FlowStep();
        step.setStepOrder(1);
        step.setStepType("TEXT");
        step.setBody("Here is your link");

        when(igAccounts.findByIgUserId(IG_USER_ID)).thenReturn(Optional.of(acc));
        when(automations.findByIgAccountIdAndTypeAndStatus(1L, "COMMENT", "ACTIVE"))
                .thenReturn(List.of(automation));
        when(steps.findByAutomationIdOrderByStepOrderAsc(11L)).thenReturn(List.of(step));
        when(logs.save(any(MessageLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(leads.findByUserIdAndAutomationIdAndIgUserId(eq(USER_ID), eq(11L), any()))
                .thenReturn(Optional.empty());
        when(leads.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));
        when(users.findById(USER_ID)).thenReturn(Optional.of(new AppUser()));
        when(usage.canSend(any())).thenReturn(true);
        when(instagram.sendPrivateReply(ACCESS_TOKEN, "comment-99", "Here is your link"))
                .thenReturn("resolved-igsid");

        engine.handleComment(IG_USER_ID, "media-1", "comment-99", FROM_IGSID, "financewithveera",
                "review", "COMMENT");

        verify(instagram).sendPrivateReply(ACCESS_TOKEN, "comment-99", "Here is your link");
        verify(instagram, never()).sendText(any(), any(), any());
    }

    private InstagramAccount connectedAccount() {
        InstagramAccount acc = new InstagramAccount();
        acc.setId(1L);
        acc.setUserId(USER_ID);
        acc.setIgUserId(IG_USER_ID);
        acc.setPageAccessToken(ACCESS_TOKEN);
        return acc;
    }

    private Automation dmAutomation(long id, String name) {
        Automation a = new Automation();
        a.setId(id);
        a.setUserId(USER_ID);
        a.setIgAccountId(1L);
        a.setName(name);
        a.setType("DM");
        a.setStatus("ACTIVE");
        return a;
    }

    private TriggerKeyword keyword(String word, String matchType) {
        TriggerKeyword k = new TriggerKeyword();
        k.setKeyword(word);
        k.setMatchType(matchType);
        return k;
    }

    private String findLog(Level level) {
        return logAppender.list.stream()
                .filter(event -> event.getLevel() == level)
                .map(ILoggingEvent::getFormattedMessage)
                .findFirst()
                .orElseThrow();
    }

    private boolean hasInfo(String fragment) {
        return logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .anyMatch(msg -> msg.contains(fragment));
    }

    private boolean hasWarn(String fragment) {
        return logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .anyMatch(msg -> msg.contains(fragment));
    }

    private String allLogs() {
        return logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + b);
    }
}
