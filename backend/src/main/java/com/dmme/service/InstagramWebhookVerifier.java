package com.dmme.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class InstagramWebhookVerifier {

    private static final String HEADER_PREFIX = "sha256=";
    private static final int SHA256_DIGEST_BYTES = 32;

    public sealed interface VerificationResult {
        record Success(int matchedSecretIndex) implements VerificationResult {}

        record MissingHeader() implements VerificationResult {}

        record MalformedHeader(Malformation reason) implements VerificationResult {
            public enum Malformation {
                MISSING_PREFIX,
                EMPTY_DIGEST,
                INVALID_HEX,
                UNEXPECTED_DIGEST_LENGTH
            }
        }

        record ComputedCandidate(int index, String digestPrefix) {}

        record NoMatch(
                int candidatesTried,
                int bodyLengthBytes,
                String receivedDigestFingerprint,
                String bodyHashPrefix,
                List<ComputedCandidate> computedByCandidate) implements VerificationResult {}
    }

    /** Verify {@code X-Hub-Signature-256} against any of the given secrets (first match wins). */
    public VerificationResult verifyAny(byte[] rawBody, String signatureHeader, List<String> secrets) {
        if (signatureHeader == null) {
            return new VerificationResult.MissingHeader();
        }

        MalformedHeaderParse malformed = parseMalformedHeader(signatureHeader);
        if (malformed != null) {
            return new VerificationResult.MalformedHeader(malformed.reason());
        }

        String receivedHex = signatureHeader.substring(HEADER_PREFIX.length());
        byte[] received = HexFormat.of().parseHex(receivedHex);

        for (int i = 0; i < secrets.size(); i++) {
            byte[] expected = hmacSha256Bytes(rawBody, secrets.get(i));
            if (MessageDigest.isEqual(expected, received)) {
                return new VerificationResult.Success(i);
            }
        }

        List<VerificationResult.NoMatch.ComputedCandidate> computed = new ArrayList<>(secrets.size());
        for (int i = 0; i < secrets.size(); i++) {
            String computedHex = HexFormat.of().formatHex(hmacSha256Bytes(rawBody, secrets.get(i)));
            computed.add(new VerificationResult.NoMatch.ComputedCandidate(
                    i, InstagramWebhookDiagnostics.digestFingerprint(computedHex)));
        }
        return new VerificationResult.NoMatch(
                secrets.size(),
                rawBody.length,
                InstagramWebhookDiagnostics.digestFingerprint(receivedHex),
                InstagramWebhookDiagnostics.bodyHashPrefix(rawBody),
                List.copyOf(computed));
    }

    /** Verify {@code X-Hub-Signature-256} against the raw webhook body bytes. */
    public boolean verify(byte[] rawBody, String signatureHeader, String appSecret) {
        VerificationResult result = verifyAny(rawBody, signatureHeader, List.of(appSecret));
        return result instanceof VerificationResult.Success;
    }

    private record MalformedHeaderParse(VerificationResult.MalformedHeader.Malformation reason) {}

    private static MalformedHeaderParse parseMalformedHeader(String signatureHeader) {
        if (!signatureHeader.startsWith(HEADER_PREFIX)) {
            return new MalformedHeaderParse(
                    VerificationResult.MalformedHeader.Malformation.MISSING_PREFIX);
        }
        String receivedHex = signatureHeader.substring(HEADER_PREFIX.length());
        if (receivedHex.isEmpty()) {
            return new MalformedHeaderParse(
                    VerificationResult.MalformedHeader.Malformation.EMPTY_DIGEST);
        }
        byte[] received;
        try {
            received = HexFormat.of().parseHex(receivedHex);
        } catch (IllegalArgumentException e) {
            return new MalformedHeaderParse(
                    VerificationResult.MalformedHeader.Malformation.INVALID_HEX);
        }
        if (received.length != SHA256_DIGEST_BYTES) {
            return new MalformedHeaderParse(
                    VerificationResult.MalformedHeader.Malformation.UNEXPECTED_DIGEST_LENGTH);
        }
        return null;
    }

    private static byte[] hmacSha256Bytes(byte[] data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data);
        } catch (java.security.NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }
}
