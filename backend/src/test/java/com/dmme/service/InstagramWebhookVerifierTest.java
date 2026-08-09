package com.dmme.service;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstagramWebhookVerifierTest {

    private static final String SECRET = "test-app-secret";
    private static final String WEBHOOK_SECRET = "test-webhook-secret";
    private final InstagramWebhookVerifier verifier = new InstagramWebhookVerifier();

    @Test
    void validSignaturePasses() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        String signature = sign(body, SECRET);

        assertTrue(verifier.verify(body, signature, SECRET));
    }

    @Test
    void tamperedBodyFails() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        String signature = sign(body, SECRET);
        byte[] tampered = "{\"entry\":[1]}".getBytes(StandardCharsets.UTF_8);

        assertFalse(verifier.verify(tampered, signature, SECRET));
    }

    @Test
    void missingHeaderIsRejected() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);

        assertFalse(verifier.verify(body, null, SECRET));
    }

    @Test
    void malformedHeaderWithoutPrefixIsRejected() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        String digest = HexFormat.of().formatHex(hmac(body, SECRET));

        assertFalse(verifier.verify(body, digest, SECRET));
    }

    @Test
    void malformedHeaderWithEmptyDigestIsRejected() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);

        assertFalse(verifier.verify(body, "sha256=", SECRET));
    }

    @Test
    void malformedHeaderWithInvalidHexIsRejected() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);

        assertFalse(verifier.verify(body, "sha256=not-valid-hex", SECRET));
    }

    @Test
    void verifyAnyAcceptsFirstMatchingSecret() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        String signature = sign(body, WEBHOOK_SECRET);

        assertTrue(verifier.verifyAny(body, signature, List.of(WEBHOOK_SECRET, SECRET)));
    }

    @Test
    void verifyAnyAcceptsFallbackSecret() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        String signature = sign(body, SECRET);

        assertTrue(verifier.verifyAny(body, signature, List.of(WEBHOOK_SECRET, SECRET)));
    }

    @Test
    void verifyAnyRejectsWhenNoSecretMatches() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        String signature = sign(body, "other-secret");

        assertFalse(verifier.verifyAny(body, signature, List.of(WEBHOOK_SECRET, SECRET)));
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
