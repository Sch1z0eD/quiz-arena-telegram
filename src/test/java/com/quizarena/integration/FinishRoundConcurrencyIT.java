package com.quizarena.integration;

import com.quizarena.domain.OptionOrder;
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

class FinishRoundConcurrencyIT extends AbstractIntegrationTest {

    private static final int ROUNDS = 50;
    private static final int THREADS = 16;

    @Autowired
    private GameStore store;

    @Test
    void casFinalizesEachRoundExactlyOnceUnderRealConcurrency() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                long chatId = 100_000L + round;
                long token = store.nextToken();
                // Arm the round: the round key now holds this token.
                store.beginQuestion(chatId, 0, 0, OptionOrder.identity(), token, System.currentTimeMillis());

                CountDownLatch gate = new CountDownLatch(1);
                List<Future<Boolean>> attempts = new ArrayList<>();
                for (int t = 0; t < THREADS; t++) {
                    attempts.add(pool.submit(() -> {
                        gate.await();
                        // In production both the timer path and the all-answered path call finishRound with
                        // the SAME token; the real finish_round.lua CAS must let only one of them win.
                        return store.finishRound(chatId, token);
                    }));
                }
                gate.countDown(); // release every thread at once for a genuine race

                int finalizers = 0;
                for (Future<Boolean> attempt : attempts) {
                    if (attempt.get()) {
                        finalizers++;
                    }
                }
                assertEquals(1, finalizers, "round " + round + ": exactly one caller may finalize the round");
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
