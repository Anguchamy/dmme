package com.dmme.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class InstagramWebhookVerifier {

    private static final String HEADER_PREFIX = "sha256=";

    /** Verify {@code X-Hub-Signature-256} against the raw webhook body bytes. */
    public boolean verify(byte[] rawBody, String signatureHeader, String appSecret) {
        if (signatureHeader == null || !signatureHeader.startsWith(HEADER_PREFIX)) {
            return false;
        }
        String receivedHex = signatureHeader.substring(HEADER_PREFIX.length());
        if (receivedHex.isEmpty()) {
            return false;
        }
        try {
            byte[] expected = hmacSha256(rawBody, appSecret);
            byte[] received = HexFormat.of().parseHex(receivedHex);
            return MessageDigest.isEqual(expected, received);
        } catch (IllegalArgumentException | java.security.InvalidKeyException
                 | java.security.NoSuchAlgorithmException e) {
            return false;
        }
    }

    private static byte[] hmacSha256(byte[] data, String secret)
            throws java.security.NoSuchAlgorithmException, java.security.InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(data);
    }
}
