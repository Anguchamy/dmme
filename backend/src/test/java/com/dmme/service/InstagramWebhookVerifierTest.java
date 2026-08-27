package com.dmme.service;

import com.dmme.service.InstagramWebhookVerifier.VerificationResult;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

        VerificationResult result = verifier.verifyAny(tampered, signature, List.of(SECRET));
        assertInstanceOf(VerificationResult.NoMatch.class, result);
    }

    @Test
    void missingHeaderIsRejected() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);

        VerificationResult result = verifier.verifyAny(body, null, List.of(SECRET));
        assertInstanceOf(VerificationResult.MissingHeader.class, result);
    }

    @Test
    void malformedHeaderWithoutPrefixIsRejected() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        String digest = HexFormat.of().formatHex(hmac(body, SECRET));

        VerificationResult result = verifier.verifyAny(body, digest, List.of(SECRET));
        assertEquals(
                VerificationResult.MalformedHeader.Malformation.MISSING_PREFIX,
                ((VerificationResult.MalformedHeader) result).reason());
    }

    @Test
    void malformedHeaderWithEmptyDigestIsRejected() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);

        VerificationResult result = verifier.verifyAny(body, "sha256=", List.of(SECRET));
        assertEquals(
                VerificationResult.MalformedHeader.Malformation.EMPTY_DIGEST,
                ((VerificationResult.MalformedHeader) result).reason());
    }

    @Test
    void malformedHeaderWithInvalidHexIsRejected() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);

        VerificationResult result = verifier.verifyAny(body, "sha256=not-valid-hex", List.of(SECRET));
        assertEquals(
                VerificationResult.MalformedHeader.Malformation.INVALID_HEX,
                ((VerificationResult.MalformedHeader) result).reason());
    }

    @Test
    void malformedHeaderWithUnexpectedDigestLengthIsRejected() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);

        VerificationResult result = verifier.verifyAny(body, "sha256=deadbeef", List.of(SECRET));
        assertEquals(
                VerificationResult.MalformedHeader.Malformation.UNEXPECTED_DIGEST_LENGTH,
                ((VerificationResult.MalformedHeader) result).reason());
    }

    @Test
    void verifyAnyAcceptsFirstMatchingSecret() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        String signature = sign(body, WEBHOOK_SECRET);

        VerificationResult result = verifier.verifyAny(body, signature, List.of(WEBHOOK_SECRET, SECRET));
        assertEquals(0, ((VerificationResult.Success) result).matchedSecretIndex());
    }

    @Test
    void verifyAnyAcceptsFallbackSecret() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        String signature = sign(body, SECRET);

        VerificationResult result = verifier.verifyAny(body, signature, List.of(WEBHOOK_SECRET, SECRET));
        assertEquals(1, ((VerificationResult.Success) result).matchedSecretIndex());
    }

    @Test
    void verifyAnyRejectsWhenNoSecretMatches() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        String signature = sign(body, "other-secret");

        VerificationResult result = verifier.verifyAny(body, signature, List.of(WEBHOOK_SECRET, SECRET));
        VerificationResult.NoMatch noMatch = assertInstanceOf(VerificationResult.NoMatch.class, result);
        assertEquals(2, noMatch.candidatesTried());
        assertEquals(body.length, noMatch.bodyLengthBytes());
        assertEquals(8, noMatch.receivedDigestFingerprint().length());
        assertEquals(2, noMatch.computedByCandidate().size());
        assertEquals(0, noMatch.computedByCandidate().get(0).index());
        assertEquals(1, noMatch.computedByCandidate().get(1).index());
        assertEquals(8, noMatch.computedByCandidate().get(0).digestPrefix().length());
        assertEquals(8, noMatch.computedByCandidate().get(1).digestPrefix().length());
        assertNotEquals(
                noMatch.computedByCandidate().get(0).digestPrefix(),
                noMatch.computedByCandidate().get(1).digestPrefix());
        assertEquals(12, noMatch.bodyHashPrefix().length());
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
