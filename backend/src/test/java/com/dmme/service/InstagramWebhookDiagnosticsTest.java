package com.dmme.service;

import com.dmme.service.InstagramWebhookDiagnostics.ConfiguredSecret;
import com.dmme.service.InstagramWebhookVerifier.VerificationResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstagramWebhookDiagnosticsTest {

    private static final String SECRET = "test-app-secret";
    private static final String WEBHOOK_SECRET = "test-webhook-secret";

    @Test
    void secretFingerprintIsStableSha256PrefixWithLength() throws Exception {
        String expectedPrefix = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(SECRET.getBytes(StandardCharsets.UTF_8)))
                .substring(0, InstagramWebhookDiagnostics.SECRET_FINGERPRINT_HEX_CHARS);

        assertEquals("fp=" + expectedPrefix + " len=" + SECRET.length(), InstagramWebhookDiagnostics.secretFingerprint(SECRET));
        assertEquals(InstagramWebhookDiagnostics.secretFingerprint(SECRET), InstagramWebhookDiagnostics.secretFingerprint(SECRET));
    }

    @Test
    void secretFingerprintIsNotDerivedFromSecretPrefix() {
        String fingerprint = InstagramWebhookDiagnostics.secretFingerprint(SECRET);
        String hashPrefix = fingerprint.substring("fp=".length(), fingerprint.indexOf(" len="));

        assertFalse(SECRET.startsWith(hashPrefix));
        assertNotEquals(SECRET.substring(0, InstagramWebhookDiagnostics.SECRET_FINGERPRINT_HEX_CHARS), hashPrefix);
    }

    @Test
    void bodyIntegrityIncludesHashPrefixAndLength() throws Exception {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        String expectedHashPrefix = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(body))
                .substring(0, InstagramWebhookDiagnostics.BODY_HASH_HEX_CHARS);

        assertEquals("bodyHash=" + expectedHashPrefix + " len=" + body.length, InstagramWebhookDiagnostics.bodyIntegrity(body));
    }

    @Test
    void formatStartupSummaryListsConfiguredSecretsWithFingerprints() {
        List<ConfiguredSecret> configured = List.of(
                new ConfiguredSecret(0, "webhook app secret", WEBHOOK_SECRET),
                new ConfiguredSecret(1, "OAuth app secret (fallback)", SECRET));

        String summary = InstagramWebhookDiagnostics.formatStartupSummary(configured);

        assertTrue(summary.contains("webhook app secret fp="));
        assertTrue(summary.contains("len=" + WEBHOOK_SECRET.length()));
        assertTrue(summary.contains("OAuth app secret (fallback) fp="));
        assertTrue(summary.contains("len=" + SECRET.length()));
    }

    @Test
    void formatStartupSummaryWhenNoneConfigured() {
        assertEquals(
                "none configured (signature verification disabled)",
                InstagramWebhookDiagnostics.formatStartupSummary(List.of()));
    }

    @Test
    void formatNoMatchMessageIncludesEveryCandidateDigestBodyHashAndFingerprints() {
        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        VerificationResult.NoMatch noMatch = new VerificationResult.NoMatch(
                2,
                body.length,
                "8c3cbf63",
                InstagramWebhookDiagnostics.bodyHashPrefix(body),
                List.of(
                        new VerificationResult.NoMatch.ComputedCandidate(0, "4275e06f"),
                        new VerificationResult.NoMatch.ComputedCandidate(1, "91ab2cd3")));
        List<ConfiguredSecret> configured = List.of(
                new ConfiguredSecret(0, "webhook app secret", WEBHOOK_SECRET),
                new ConfiguredSecret(1, "OAuth app secret (fallback)", SECRET));

        String message = InstagramWebhookDiagnostics.formatNoMatchMessage(noMatch, configured);

        assertTrue(message.contains("body " + body.length + " bytes, bodyHash="));
        assertTrue(message.contains("received digest prefix=8c3cbf63"));
        assertTrue(message.contains("candidate[0] webhook app secret fp="));
        assertTrue(message.contains("computed=4275e06f"));
        assertTrue(message.contains("candidate[1] OAuth app secret (fallback) fp="));
        assertTrue(message.contains("computed=91ab2cd3"));
        assertTrue(message.contains("len=" + WEBHOOK_SECRET.length()));
        assertTrue(message.contains("len=" + SECRET.length()));
    }
}
