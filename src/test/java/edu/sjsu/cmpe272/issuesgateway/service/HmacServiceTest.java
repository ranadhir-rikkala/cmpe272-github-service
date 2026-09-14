package edu.sjsu.cmpe272.issuesgateway.service;

/*
 * Author: Ranadhir Reddy Rikkala
 * Contribution: Unit tests for webhook HMAC signature verification.
 */

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HmacServiceTest {

    private static final String SECRET = "test-webhook-secret";

    private HmacService hmacService;

    @BeforeEach
    void setUp() {
        hmacService = new HmacService();
        ReflectionTestUtils.setField(hmacService, "secret", SECRET);
    }

    private String sign(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        return "sha256=" + HexFormat.of().formatHex(digest);
    }

    @Test
    void validSignatureIsAccepted() throws Exception {
        String body = "{\"action\":\"opened\",\"issue\":{\"number\":1}}";

        boolean result = hmacService.verifySignature(
                body.getBytes(StandardCharsets.UTF_8),
                sign(body, SECRET)
        );

        assertTrue(result);
    }

    @Test
    void signatureFromWrongSecretIsRejected() throws Exception {
        String body = "{\"action\":\"opened\"}";

        boolean result = hmacService.verifySignature(
                body.getBytes(StandardCharsets.UTF_8),
                sign(body, "the-wrong-secret")
        );

        assertFalse(result);
    }

    @Test
    void tamperedBodyIsRejected() throws Exception {
        String original = "{\"action\":\"opened\",\"issue\":{\"number\":1}}";
        String tampered = "{\"action\":\"deleted\",\"issue\":{\"number\":1}}";

        String signatureOfOriginal = sign(original, SECRET);

        boolean result = hmacService.verifySignature(
                tampered.getBytes(StandardCharsets.UTF_8),
                signatureOfOriginal
        );

        assertFalse(result);
    }

    @Test
    void missingSignatureHeaderIsRejected() {
        boolean result = hmacService.verifySignature(
                "{}".getBytes(StandardCharsets.UTF_8),
                null
        );

        assertFalse(result);
    }

    @Test
    void signatureWithoutSha256PrefixIsRejected() throws Exception {
        String body = "{\"action\":\"opened\"}";
        String withoutPrefix = sign(body, SECRET).substring("sha256=".length());

        boolean result = hmacService.verifySignature(
                body.getBytes(StandardCharsets.UTF_8),
                withoutPrefix
        );

        assertFalse(result);
    }

    @Test
    void malformedSignatureIsRejected() {
        boolean result = hmacService.verifySignature(
                "{}".getBytes(StandardCharsets.UTF_8),
                "sha256=not-a-valid-hex-digest"
        );

        assertFalse(result);
    }

    @Test
    void emptyBodyStillVerifiesConsistently() throws Exception {
        String body = "";

        boolean result = hmacService.verifySignature(
                body.getBytes(StandardCharsets.UTF_8),
                sign(body, SECRET)
        );

        assertTrue(result);
    }
}
