package com.dmme.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dmme.config.AppProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstagramClientTest {

    private static final String ACCESS_TOKEN = "IGQVJ-test-access-token-do-not-log";
    private static final String IG_USER_ID = "17841448037418309";

    private MockWebServer server;
    private InstagramClient client;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        AppProperties props = new AppProperties();
        String baseUrl = server.url("/").toString();
        props.getInstagram().setGraphBaseUrl(baseUrl.substring(0, baseUrl.length() - 1));
        props.getInstagram().setIgGraphBaseUrl(baseUrl.substring(0, baseUrl.length() - 1));
        client = new InstagramClient(props);

        Logger logger = (Logger) LoggerFactory.getLogger(InstagramClient.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() throws IOException {
        Logger logger = (Logger) LoggerFactory.getLogger(InstagramClient.class);
        logger.detachAppender(logAppender);
        server.shutdown();
    }

    @Test
    void listMediaOAuthExceptionProducesAuthFlaggedException() {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("""
                        {"error":{"message":"Error validating access token: Session has expired","type":"OAuthException","code":190,"error_subcode":463,"fbtrace_id":"AbCdEf123"}}
                        """));

        InstagramApiException ex = assertThrows(InstagramApiException.class,
                () -> client.listMedia(ACCESS_TOKEN, IG_USER_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("/" + IG_USER_ID + "/media", ex.getRequestPath());
        assertEquals("OAuthException", ex.getGraphType());
        assertEquals(190, ex.getGraphCode());
        assertEquals(463, ex.getErrorSubcode());
        assertEquals("AbCdEf123", ex.getFbtraceId());
        assertTrue(ex.isAuthProblem());
        assertLogDoesNotContainToken();
    }

    @Test
    void listMediaEmptyErrorBodyStillProducesExceptionWithStatusAndPath() {
        server.enqueue(new MockResponse().setResponseCode(400));

        InstagramApiException ex = assertThrows(InstagramApiException.class,
                () -> client.listMedia(ACCESS_TOKEN, IG_USER_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("/" + IG_USER_ID + "/media", ex.getRequestPath());
        assertNull(ex.getGraphType());
        assertNull(ex.getGraphCode());
        assertFalse(ex.isAuthProblem());
        assertLogDoesNotContainToken();
    }

    @Test
    void listMediaUnparseableErrorBodyStillProducesExceptionWithStatusAndPath() {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("not-json"));

        InstagramApiException ex = assertThrows(InstagramApiException.class,
                () -> client.listMedia(ACCESS_TOKEN, IG_USER_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("/" + IG_USER_ID + "/media", ex.getRequestPath());
        assertNull(ex.getGraphMessage());
        assertFalse(ex.isAuthProblem());
        assertLogDoesNotContainToken();
    }

    @Test
    void postGraph500ProducesNonAuthException() {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("""
                        {"error":{"message":"An unknown error occurred","type":"GraphMethodException","code":100,"fbtrace_id":"trace-500"}}
                        """));

        InstagramApiException ex = assertThrows(InstagramApiException.class,
                () -> client.sendText(ACCESS_TOKEN, "recipient-123", "hello"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatus());
        assertEquals("/me/messages", ex.getRequestPath());
        assertEquals("GraphMethodException", ex.getGraphType());
        assertEquals(100, ex.getGraphCode());
        assertFalse(ex.isAuthProblem());
        assertLogDoesNotContainToken();
    }

    @Test
    void isFollowerSwallowsAuthErrorsAndReturnsFalse() {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("""
                        {"error":{"message":"Invalid token","type":"OAuthException","code":190,"error_subcode":463,"fbtrace_id":"swallowed"}}
                        """));

        assertFalse(client.isFollower(ACCESS_TOKEN, IG_USER_ID, "igsid-1"));
    }

    private void assertLogDoesNotContainToken() {
        String logs = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + b);
        assertFalse(logs.contains(ACCESS_TOKEN), "access token must not appear in logs");
    }
}
