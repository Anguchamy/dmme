package com.dmme.web;

import com.dmme.config.AppProperties;
import com.dmme.service.AutomationEngine;
import com.dmme.service.InstagramWebhookVerifier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
    private InstagramWebhookVerifier verifier;
    @Mock
    private ObjectMapper objectMapper;

    private InstagramWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new InstagramWebhookController(props, engine, verifier, objectMapper);
        when(props.getInstagram()).thenReturn(instagram);
    }

    @Test
    void blankSecretAllowsRequestThrough() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(null);
        when(instagram.getAppSecret()).thenReturn("  ");
        when(objectMapper.readValue(eq(BODY), any(TypeReference.class)))
                .thenReturn(Map.of("entry", List.of()));

        ResponseEntity<String> response = controller.receive(BODY, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(verifier, never()).verifyAny(any(), any(), any());
    }

    @Test
    void validSignatureWithWebhookSecretPasses() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(WEBHOOK_SECRET);
        when(instagram.getAppSecret()).thenReturn(OAUTH_SECRET);
        String signature = sign(BODY, WEBHOOK_SECRET);
        when(verifier.verifyAny(BODY, signature, List.of(WEBHOOK_SECRET, OAUTH_SECRET))).thenReturn(true);
        when(objectMapper.readValue(eq(BODY), any(TypeReference.class)))
                .thenReturn(Map.of("entry", List.of()));

        ResponseEntity<String> response = controller.receive(BODY, signature);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void validSignatureWithOnlyOAuthSecretPasses() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(null);
        when(instagram.getAppSecret()).thenReturn(OAUTH_SECRET);
        String signature = sign(BODY, OAUTH_SECRET);
        when(verifier.verifyAny(BODY, signature, List.of(OAUTH_SECRET))).thenReturn(true);
        when(objectMapper.readValue(eq(BODY), any(TypeReference.class)))
                .thenReturn(Map.of("entry", List.of()));

        ResponseEntity<String> response = controller.receive(BODY, signature);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void missingSignatureRejectedWhenSecretConfigured() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(WEBHOOK_SECRET);
        when(instagram.getAppSecret()).thenReturn(OAUTH_SECRET);

        ResponseEntity<String> response = controller.receive(BODY, null);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(objectMapper, never()).readValue(any(byte[].class), any(TypeReference.class));
    }

    @Test
    void invalidSignatureRejectedWhenSecretConfigured() throws Exception {
        when(instagram.getWebhookAppSecret()).thenReturn(WEBHOOK_SECRET);
        when(instagram.getAppSecret()).thenReturn(OAUTH_SECRET);
        when(verifier.verifyAny(BODY, "sha256=deadbeef", List.of(WEBHOOK_SECRET, OAUTH_SECRET)))
                .thenReturn(false);

        ResponseEntity<String> response = controller.receive(BODY, "sha256=deadbeef");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(objectMapper, never()).readValue(any(byte[].class), any(TypeReference.class));
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
