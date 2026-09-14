package edu.sjsu.cmpe272.issuesgateway.service;

/*
 * Author: Mukesh Singh
 * Contribution: HMAC SHA-256 signature verification with constant-time comparison
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Service
public class HmacService {

    @Value("${github.webhook.secret}")
    private String secret;

    public boolean verifySignature(byte[] rawBody, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }

        String expectedHash = signatureHeader.substring(7);

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);

            byte[] computedHashBytes = mac.doFinal(rawBody);
            String computedHash = bytesToHex(computedHashBytes);

            // Constant-time byte array comparison
            return MessageDigest.isEqual(
                computedHash.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
