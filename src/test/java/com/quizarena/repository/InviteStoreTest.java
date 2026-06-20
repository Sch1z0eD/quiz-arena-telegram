package com.quizarena.repository;

import com.quizarena.domain.DuelInvite;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InviteStoreTest {

    @Test
    void encodeDecodeRoundTrip() {
        DuelInvite invite = new DuelInvite(123L, -456L, "ru", "", "medium", "Анна <co> & друзья");
        assertEquals(invite, InviteStore.decode(InviteStore.encode(invite)));
    }

    @Test
    void encodeDecodeRoundTripWithAnyFilters() {
        // Inline invites carry no picker, so both category and difficulty are empty (two empty segments).
        DuelInvite invite = new DuelInvite(7L, 7L, "en", "", "", "Bob");
        assertEquals(invite, InviteStore.decode(InviteStore.encode(invite)));
    }

    @Test
    void claimIsOneTime() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        String encoded = InviteStore.encode(new DuelInvite(1L, 2L, "en", "science", "hard", "Bob"));
        when(ops.getAndDelete("duel:invite:tok")).thenReturn(encoded).thenReturn(null);

        InviteStore store = new InviteStore(redis);
        assertTrue(store.claim("tok").isPresent(), "first claim wins");
        assertTrue(store.claim("tok").isEmpty(), "forwarded or reused link must be blocked");
    }
}
