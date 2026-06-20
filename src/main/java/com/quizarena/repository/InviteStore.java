package com.quizarena.repository;

import com.quizarena.domain.DuelInvite;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InviteStore {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;

    public InviteStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String create(DuelInvite invite) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(key(token), encode(invite), TTL);
        return token;
    }

    // Atomic one-time claim: GETDEL means a forwarded or reused link wins at most once.
    public Optional<DuelInvite> claim(String token) {
        String raw = redis.opsForValue().getAndDelete(key(token));
        return raw == null ? Optional.empty() : Optional.of(decode(raw));
    }

    public void cancel(String token) {
        redis.delete(key(token));
    }

    static String encode(DuelInvite invite) {
        return invite.inviterUserId() + ":" + invite.inviterChatId() + ":" + invite.locale() + ":"
                + invite.category() + ":" + invite.difficulty() + ":"
                + Base64.getEncoder().encodeToString(invite.inviterName().getBytes(StandardCharsets.UTF_8));
    }

    static DuelInvite decode(String raw) {
        String[] p = raw.split(":", -1);
        String name = new String(Base64.getDecoder().decode(p[5]), StandardCharsets.UTF_8);
        return new DuelInvite(Long.parseLong(p[0]), Long.parseLong(p[1]), p[2], p[3], p[4], name);
    }

    private static String key(String token) {
        return "duel:invite:" + token;
    }
}
