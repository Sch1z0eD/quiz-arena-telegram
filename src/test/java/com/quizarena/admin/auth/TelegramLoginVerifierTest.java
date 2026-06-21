package com.quizarena.admin.auth;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegramLoginVerifierTest {

    private static final String TOKEN = "123456:test-bot-token";
    private static final Instant NOW = Instant.parse("2026-06-21T10:00:00Z");

    private final TelegramLoginVerifier verifier =
            new TelegramLoginVerifier(TOKEN, List.of(777L), 86400, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void acceptsValidSignedPayload() {
        Optional<VerifiedAdmin> result = verifier.verify(signed(Map.of(
                "id", "777", "first_name", "Alice", "auth_date", Long.toString(NOW.getEpochSecond()))));
        assertTrue(result.isPresent());
        assertEquals(777L, result.get().id());
        assertEquals("Alice", result.get().name());
    }

    @Test
    void rejectsTamperedField() {
        Map<String, String> params = signed(Map.of(
                "id", "777", "first_name", "Alice", "auth_date", Long.toString(NOW.getEpochSecond())));
        params.put("first_name", "Mallory");
        assertTrue(verifier.verify(params).isEmpty());
    }

    @Test
    void rejectsTamperedHash() {
        Map<String, String> params = signed(Map.of(
                "id", "777", "first_name", "Alice", "auth_date", Long.toString(NOW.getEpochSecond())));
        params.put("hash", "00000000000000000000000000000000000000000000000000000000deadbeef");
        assertTrue(verifier.verify(params).isEmpty());
    }

    @Test
    void rejectsStaleAuthDate() {
        Map<String, String> params = signed(Map.of(
                "id", "777", "first_name", "Alice", "auth_date", Long.toString(NOW.getEpochSecond() - 200_000)));
        assertTrue(verifier.verify(params).isEmpty());
    }

    @Test
    void rejectsNonAllowlistedId() {
        Map<String, String> params = signed(Map.of(
                "id", "999", "first_name", "Eve", "auth_date", Long.toString(NOW.getEpochSecond())));
        assertTrue(verifier.verify(params).isEmpty());
    }

    @Test
    void rejectsMissingHash() {
        Map<String, String> params = new HashMap<>(Map.of(
                "id", "777", "auth_date", Long.toString(NOW.getEpochSecond())));
        assertTrue(verifier.verify(params).isEmpty());
    }

    private static Map<String, String> signed(Map<String, String> fields) {
        Map<String, String> params = new HashMap<>(fields);
        String dataCheckString = new TreeMap<>(params).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        params.put("hash", HexFormat.of().formatHex(hmac(sha256(TOKEN), dataCheckString)));
        return params;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
