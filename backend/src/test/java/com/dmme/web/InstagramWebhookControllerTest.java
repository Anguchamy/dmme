package com.dmme.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dmme.config.AppProperties;
import com.dmme.service.AutomationEngine;
import com.dmme.service.InstagramWebhookVerifier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstagramWebhookControllerTest {

    private static final byte[] BODY = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
    private static final String WEBHOOK_SECRET = "webhook-secret";
    private static final String OAUTH_SECRET = "oauth-secret";

    @Mock
    private AppProperties props;
    @Mock
    private AppProperties.Instagram instagram;
    @Mock
    private AutomationEngine engine;
    @Mock
    private ObjectMapper objectMapper;

    private final InstagramWebhookVerifier verifier = new InstagramWebhookVerifier();
    private InstagramWebhookController controller;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        controller = new InstagramWebhookController(props, engine, verifier, objectMapper);
        when(props.getInstagram()).thenReturn(instagram);

        Logger logger = (Logger) LoggerFactory.getLogger(InstagramWebhookController.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(InstagramWebhookController.class);
        logger.detachAppender(logAppender);
    }

    @Test
    void blankSecretAllowsRequestThrough() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(null);
        when(instagram.getAppSecret()).thenReturn("  ");
        when(objectMapper.readValue(eq(BODY), any(TypeReference.class)))
                .thenReturn(Map.of("entry", List.of()));

        ResponseEntity<String> response = controller.receive(BODY, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void validSignatureWithWebhookSecretPasses() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(WEBHOOK_SECRET);
        when(instagram.getAppSecret()).thenReturn(OAUTH_SECRET);
        String signature = sign(BODY, WEBHOOK_SECRET);
        when(objectMapper.readValue(eq(BODY), any(TypeReference.class)))
                .thenReturn(Map.of("entry", List.of()));

        ResponseEntity<String> response = controller.receive(BODY, signature, "facebookexternalua");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void validSignatureWithOnlyOAuthSecretPasses() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(null);
        when(instagram.getAppSecret()).thenReturn(OAUTH_SECRET);
        String signature = sign(BODY, OAUTH_SECRET);
        when(objectMapper.readValue(eq(BODY), any(TypeReference.class)))
                .thenReturn(Map.of("entry", List.of()));

        ResponseEntity<String> response = controller.receive(BODY, signature, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void validSignatureWithFallbackOAuthSecretPasses() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(WEBHOOK_SECRET);
        when(instagram.getAppSecret()).thenReturn(OAUTH_SECRET);
        String signature = sign(BODY, OAUTH_SECRET);
        when(objectMapper.readValue(eq(BODY), any(TypeReference.class)))
                .thenReturn(Map.of("entry", List.of()));

        ResponseEntity<String> response = controller.receive(BODY, signature, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void missingSignatureRejectedWhenSecretConfigured() {
        when(instagram.getWebhookAppSecret()).thenReturn(WEBHOOK_SECRET);
        when(instagram.getAppSecret()).thenReturn(OAUTH_SECRET);

        ResponseEntity<String> response = controller.receive(BODY, null, "curl/8.4.0");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void malformedSignatureRejectedWhenSecretConfigured() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(WEBHOOK_SECRET);
        when(instagram.getAppSecret()).thenReturn(OAUTH_SECRET);

        ResponseEntity<String> response = controller.receive(BODY, "sha256=not-valid-hex", null);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(objectMapper, never()).readValue(any(byte[].class), any(TypeReference.class));
    }

    @Test
    void noMatchingSignatureRejectedWhenSecretConfigured() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(WEBHOOK_SECRET);
        when(instagram.getAppSecret()).thenReturn(OAUTH_SECRET);
        String signature = sign(BODY, "other-secret");

        ResponseEntity<String> response = controller.receive(BODY, signature, "facebookexternalua");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(objectMapper, never()).readValue(any(byte[].class), any(TypeReference.class));

        String warnMessage = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .findFirst()
                .orElseThrow();
        assertTrue(warnMessage.contains("candidate[0] webhook app secret fp="));
        assertTrue(warnMessage.contains("computed="));
        assertTrue(warnMessage.contains("candidate[1] OAuth app secret (fallback) fp="));
        assertTrue(warnMessage.contains("bodyHash="));
        assertTrue(warnMessage.contains("len=" + WEBHOOK_SECRET.length()));
        assertTrue(warnMessage.contains("len=" + OAUTH_SECRET.length()));
    }

    @Test
    void startupLogsConfiguredSecretFingerprints() {
        when(instagram.getWebhookAppSecret()).thenReturn(WEBHOOK_SECRET);
        when(instagram.getAppSecret()).thenReturn(OAUTH_SECRET);

        controller.logConfiguredWebhookSecretsAtStartup();

        String infoMessage = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .findFirst()
                .orElseThrow();
        assertTrue(infoMessage.startsWith("Instagram webhook secrets configured:"));
        assertTrue(infoMessage.contains("webhook app secret fp="));
        assertTrue(infoMessage.contains("len=" + WEBHOOK_SECRET.length()));
        assertTrue(infoMessage.contains("OAuth app secret (fallback) fp="));
        assertTrue(infoMessage.contains("len=" + OAUTH_SECRET.length()));
    }

    @Test
    void emptyEntryListIsReportedRatherThanIgnored() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(null);
        when(instagram.getAppSecret()).thenReturn(null);
        when(objectMapper.readValue(eq(BODY), any(TypeReference.class)))
                .thenReturn(Map.of("object", "instagram", "entry", List.of()));

        controller.receive(BODY, null, null);

        assertTrue(warnings().anyMatch(m -> m.contains("carries no entries")));
        verify(engine, never()).handleMessage(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void entryWithoutMessagingOrChangesIsReported() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(null);
        when(instagram.getAppSecret()).thenReturn(null);
        when(objectMapper.readValue(eq(BODY), any(TypeReference.class)))
                .thenReturn(Map.of("object", "instagram",
                        "entry", List.of(Map.of("id", "17841400000000000", "time", 1L))));

        controller.receive(BODY, null, null);

        assertTrue(warnings().anyMatch(m ->
                m.contains("neither messaging nor changes") && m.contains("17841400000000000")));
    }

    @Test
    void unhandledChangeFieldIsReportedWithItsName() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(null);
        when(instagram.getAppSecret()).thenReturn(null);
        when(objectMapper.readValue(eq(BODY), any(TypeReference.class)))
                .thenReturn(Map.of("object", "instagram", "entry", List.of(Map.of(
                        "id", "17841400000000000",
                        "changes", List.of(Map.of("field", "story_insights", "value", Map.of()))))));

        controller.receive(BODY, null, null);

        assertTrue(warnings().anyMatch(m ->
                m.contains("field=story_insights") && m.contains("only comments and live_comments")));
        verify(engine, never()).handleComment(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void messagingEventWithoutTextIsFlaggedAsUnmatchable() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(null);
        when(instagram.getAppSecret()).thenReturn(null);
        when(objectMapper.readValue(eq(BODY), any(TypeReference.class)))
                .thenReturn(Map.of("object", "instagram", "entry", List.of(Map.of(
                        "id", "17841400000000000",
                        "messaging", List.of(Map.of(
                                "sender", Map.of("id", "999"),
                                "message", Map.of("mid", "abc")))))));

        controller.receive(BODY, null, null);

        assertTrue(logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .anyMatch(m -> m.contains("carries no text")));
        verify(engine).handleMessage(eq("17841400000000000"), eq("999"), any(), eq(null), eq(false));
    }

    private java.util.stream.Stream<String> warnings() {
        return logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage);
    }

    private static String sign(byte[] body, String secret) {
        return "sha256=" + HexFormat.of().formatHex(hmac(body, secret));
    }

    private static byte[] hmac(byte[] body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
