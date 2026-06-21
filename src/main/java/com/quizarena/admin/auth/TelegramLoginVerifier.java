package com.quizarena.admin.auth;

import com.quizarena.admin.config.AdminPanelProperties;
import com.quizarena.config.TelegramProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Verifies a Telegram Login Widget payload: HMAC-SHA256 over the sorted data-check-string with a key of
 * SHA256(botToken), plus auth_date freshness and an admin allowlist. The bot token never leaves the server.
 */
@Component
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class TelegramLoginVerifier {

    private static final long FUTURE_SKEW_SECONDS = 60;

    private final byte[] secretKey;
    private final Set<Long> admins;
    private final long maxAgeSeconds;
    private final Clock clock;

    @Autowired
    public TelegramLoginVerifier(TelegramProperties bot, AdminPanelProperties panel, Clock clock) {
        this(bot.token(), panel.admins(), panel.authMaxAgeSeconds(), clock);
    }

    TelegramLoginVerifier(String botToken, List<Long> admins, long maxAgeSeconds, Clock clock) {
        this.secretKey = sha256(botToken.getBytes(StandardCharsets.UTF_8));
        this.admins = Set.copyOf(admins);
        this.maxAgeSeconds = maxAgeSeconds;
        this.clock = clock;
    }

    public Optional<VerifiedAdmin> verify(Map<String, String> params) {
        String hash = params.get("hash");
        if (hash == null || hash.isBlank()) {
            return Optional.empty();
        }
        String dataCheckString = params.entrySet().stream()
                .filter(entry -> !"hash".equals(entry.getKey()) && entry.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
        String expected = HexFormat.of()
                .formatHex(hmacSha256(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                hash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }
        Long authDate = parseLong(params.get("auth_date"));
        if (authDate == null) {
            return Optional.empty();
        }
        long now = clock.instant().getEpochSecond();
        if (now - authDate > maxAgeSeconds || authDate - now > FUTURE_SKEW_SECONDS) {
            return Optional.empty();
        }
        Long id = parseLong(params.get("id"));
        if (id == null || !admins.contains(id)) {
            return Optional.empty();
        }
        return Optional.of(new VerifiedAdmin(id, displayName(params, id)));
    }

    private static String displayName(Map<String, String> params, long id) {
        String full = (params.getOrDefault("first_name", "").trim() + " "
                + params.getOrDefault("last_name", "").trim()).trim();
        if (!full.isEmpty()) {
            return full;
        }
        String username = params.getOrDefault("username", "").trim();
        return username.isEmpty() ? Long.toString(id) : username;
    }

    private static Long parseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
