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

import java.nio.charset.StandardCharsets;
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
        when(instagram.getAppSecret()).thenReturn("  ");
        when(objectMapper.readValue(eq(BODY), any(TypeReference.class)))
                .thenReturn(Map.of("entry", List.of()));

        ResponseEntity<String> response = controller.receive(BODY, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(verifier, never()).verify(any(), any(), any());
    }

    @Test
    void missingSignatureRejectedWhenSecretConfigured() throws Exception {
        when(instagram.getAppSecret()).thenReturn("configured-secret");

        ResponseEntity<String> response = controller.receive(BODY, null);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(objectMapper, never()).readValue(any(byte[].class), any(TypeReference.class));
    }

    @Test
    void invalidSignatureRejectedWhenSecretConfigured() throws Exception {
        when(instagram.getAppSecret()).thenReturn("configured-secret");
        when(verifier.verify(BODY, "sha256=deadbeef", "configured-secret")).thenReturn(false);

        ResponseEntity<String> response = controller.receive(BODY, "sha256=deadbeef");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(objectMapper, never()).readValue(any(byte[].class), any(TypeReference.class));
    }
}
