package com.quizarena.integration;

import com.quizarena.repository.GameStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordAnswerConcurrencyIT extends AbstractIntegrationTest {

    private static final int ROUNDS = 50;
    private static final int THREADS = 16;

    @Autowired
    private GameStore store;

    @Test
    void sameUserConcurrentTapsCountExactlyOnce() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                long chatId = 200_000L + round;
                long userId = 777L;
                long token = store.nextToken();
                store.beginQuestion(chatId, 0, 0, token, System.currentTimeMillis());

                CountDownLatch gate = new CountDownLatch(1);
                List<Future<Long>> attempts = new ArrayList<>();
                for (int t = 0; t < THREADS; t++) {
                    attempts.add(pool.submit(() -> {
                        gate.await();
                        return store.recordAnswer(chatId, 0, token, userId);
                    }));
                }
                gate.countDown();

                int accepted = 0;
                for (Future<Long> attempt : attempts) {
                    long outcome = attempt.get();
                    assertTrue(outcome == 0L || outcome == 1L,
                            "round " + round + ": each tap is accepted(1) or duplicate(0), got " + outcome);
                    if (outcome == 1L) {
                        accepted++;
                    }
                }
                assertEquals(1, accepted, "round " + round + ": the same user's concurrent taps are counted once");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void staleTokenTapIsRejected() {
        long chatId = 250_000L;
        long userId = 5L;
        long token = store.nextToken();
        store.beginQuestion(chatId, 0, 0, token, System.currentTimeMillis());

        assertEquals(-1L, store.recordAnswer(chatId, 0, token + 999L, userId), "stale token -> -1");
        assertEquals(1L, store.recordAnswer(chatId, 0, token, userId), "current token -> accepted");
        assertEquals(0L, store.recordAnswer(chatId, 0, token, userId), "repeat with the same token -> duplicate");
    }
}
