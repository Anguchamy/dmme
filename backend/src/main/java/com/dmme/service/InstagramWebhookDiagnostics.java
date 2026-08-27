package com.dmme.service;

import com.dmme.service.InstagramWebhookVerifier.VerificationResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

public final class InstagramWebhookDiagnostics {

    static final int SECRET_FINGERPRINT_HEX_CHARS = 8;
    static final int BODY_HASH_HEX_CHARS = 12;
    static final int DIGEST_FINGERPRINT_HEX_CHARS = 8;

    private InstagramWebhookDiagnostics() {}

    public record ConfiguredSecret(int candidateIndex, String label, String value) {}

    public static String secretFingerprint(String secret) {
        String hashPrefix = hexPrefix(sha256Bytes(secret.getBytes(StandardCharsets.UTF_8)), SECRET_FINGERPRINT_HEX_CHARS);
        return "fp=" + hashPrefix + " len=" + secret.length();
    }

    public static String bodyIntegrity(byte[] rawBody) {
        String hashPrefix = hexPrefix(sha256Bytes(rawBody), BODY_HASH_HEX_CHARS);
        return "bodyHash=" + hashPrefix + " len=" + rawBody.length;
    }

    public static String formatStartupSummary(List<ConfiguredSecret> configuredSecrets) {
        if (configuredSecrets.isEmpty()) {
            return "none configured (signature verification disabled)";
        }
        return configuredSecrets.stream()
                .map(s -> s.label() + " " + secretFingerprint(s.value()))
                .collect(Collectors.joining("; "));
    }

    public static String formatNoMatchMessage(
            VerificationResult.NoMatch noMatch, List<ConfiguredSecret> configuredSecrets) {
        String candidates = configuredSecrets.stream()
                .map(secret -> {
                    String computed = noMatch.computedByCandidate().stream()
                            .filter(c -> c.index() == secret.candidateIndex())
                            .map(VerificationResult.NoMatch.ComputedCandidate::digestPrefix)
                            .findFirst()
                            .orElse("????????");
                    return "candidate[" + secret.candidateIndex() + "] "
                            + secret.label()
                            + " "
                            + secretFingerprint(secret.value())
                            + " computed=" + computed;
                })
                .collect(Collectors.joining("; "));
        return "Instagram webhook rejected: valid X-Hub-Signature-256 format but HMAC matched none of "
                + noMatch.candidatesTried()
                + " configured secret(s) ("
                + bodyIntegritySuffix(noMatch.bodyLengthBytes(), noMatch.bodyHashPrefix())
                + "; received digest prefix="
                + noMatch.receivedDigestFingerprint()
                + "; "
                + candidates
                + ")";
    }

    private static String bodyIntegritySuffix(int bodyLengthBytes, String bodyHashPrefix) {
        return "body " + bodyLengthBytes + " bytes, bodyHash=" + bodyHashPrefix;
    }

    static String digestFingerprint(String hexDigest) {
        return hexPrefix(hexDigest, DIGEST_FINGERPRINT_HEX_CHARS);
    }

    static String bodyHashPrefix(byte[] rawBody) {
        return hexPrefix(sha256Bytes(rawBody), BODY_HASH_HEX_CHARS);
    }

    private static String hexPrefix(String hex, int chars) {
        if (hex.length() <= chars) {
            return hex;
        }
        return hex.substring(0, chars);
    }

    private static String hexPrefix(byte[] digest, int chars) {
        return hexPrefix(HexFormat.of().formatHex(digest), chars);
    }

    private static byte[] sha256Bytes(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
