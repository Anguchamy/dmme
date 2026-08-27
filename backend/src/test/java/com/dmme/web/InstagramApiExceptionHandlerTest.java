package com.dmme.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dmme.service.InstagramApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InstagramApiExceptionHandlerTest {

    private static final String ACCESS_TOKEN = "IGQVJ-test-access-token-do-not-log";

    private final InstagramApiExceptionHandler handler = new InstagramApiExceptionHandler();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(InstagramApiExceptionHandler.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(InstagramApiExceptionHandler.class);
        logger.detachAppender(logAppender);
    }

    @Test
    void authFailureMapsTo409WithStableCode() throws Exception {
        InstagramApiException ex = InstagramApiException.parse(
                HttpStatus.BAD_REQUEST,
                "/" + "17841448037418309" + "/media",
                """
                {"error":{"message":"Error validating access token: Session has expired","type":"OAuthException","code":190,"error_subcode":463,"fbtrace_id":"AbCdEf123"}}
                """);

        ResponseEntity<Map<String, Object>> response = handler.handleInstagramApi(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals("instagram_reauth_required", body.get("code"));
        assertEquals(
                "Your Instagram connection is no longer valid. Please reconnect your Instagram account in Settings.",
                body.get("message"));
        assertEquals(190, body.get("graphCode"));
        assertEquals(463, body.get("graphSubcode"));

        String json = objectMapper.writeValueAsString(body);
        assertFalse(json.contains(ACCESS_TOKEN));
        assertFalse(json.contains("access_token"));
    }

    @Test
    void nonAuthFailureMapsTo502WithGraphMessage() {
        InstagramApiException ex = InstagramApiException.parse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "/me/messages",
                """
                {"error":{"message":"An unknown error occurred","type":"GraphMethodException","code":100,"fbtrace_id":"trace-500"}}
                """);

        ResponseEntity<Map<String, Object>> response = handler.handleInstagramApi(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("An unknown error occurred", response.getBody().get("message"));
    }
}
