package com.dmme.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstagramApiExceptionTest {

    @Test
    void parseOAuthExceptionFields() {
        InstagramApiException ex = InstagramApiException.parse(
                HttpStatus.BAD_REQUEST,
                "/17841448037418309/media",
                """
                {"error":{"message":"Error validating access token: Session has expired","type":"OAuthException","code":190,"error_subcode":463,"fbtrace_id":"AbCdEf123"}}
                """);

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("/17841448037418309/media", ex.getRequestPath());
        assertEquals("Error validating access token: Session has expired", ex.getGraphMessage());
        assertEquals("OAuthException", ex.getGraphType());
        assertEquals(190, ex.getGraphCode());
        assertEquals(463, ex.getErrorSubcode());
        assertEquals("AbCdEf123", ex.getFbtraceId());
        assertTrue(ex.isAuthProblem());
    }

    @Test
    void parseEmptyBodyKeepsStatusAndPath() {
        InstagramApiException ex = InstagramApiException.parse(
                HttpStatus.BAD_REQUEST, "/17841448037418309/media", "");

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("/17841448037418309/media", ex.getRequestPath());
        assertNull(ex.getGraphMessage());
        assertFalse(ex.isAuthProblem());
    }

    @Test
    void isAuthProblemByCode190() {
        InstagramApiException ex = InstagramApiException.parse(
                HttpStatus.BAD_REQUEST,
                "/path",
                """
                {"error":{"message":"Invalid token","type":"GraphMethodException","code":190}}
                """);

        assertTrue(ex.isAuthProblem());
    }

    @Test
    void isAuthProblemBySubcode463() {
        InstagramApiException ex = InstagramApiException.parse(
                HttpStatus.BAD_REQUEST,
                "/path",
                """
                {"error":{"message":"Session expired","type":"GraphMethodException","code":1,"error_subcode":463}}
                """);

        assertTrue(ex.isAuthProblem());
    }
}
