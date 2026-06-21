package com.quizarena.integration;

import com.quizarena.domain.OptionOrder;
import com.quizarena.repository.GameStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OptionOrderStoreIT extends AbstractIntegrationTest {

    @Autowired
    private GameStore store;

    @Test
    void roundOrderSurvivesReReadFromRedis() {
        long chatId = 920001L;
        store.startGame(chatId, List.of(1L, 2L, 3L), "science", "easy", "ru");

        // Before the first question is armed there is no qOrder yet: a round that crossed a deploy
        // must read back as identity so it scores in storage order, as it was shown.
        assertEquals(OptionOrder.identity().toCsv(), store.snapshot(chatId).order().toCsv());

        OptionOrder order = OptionOrder.parse("3,1,2,0");
        store.beginQuestion(chatId, 0, 2, order, store.nextToken(), 1000L);

        // Re-reading the snapshot from Redis (what a fresh instance after a restart would do) keeps the order.
        OptionOrder reread = store.snapshot(chatId).order();
        assertEquals("3,1,2,0", reread.toCsv());
        assertEquals(3, reread.storageAt(0));
        assertEquals(0, reread.displayOfCorrect(3));

        store.cleanup(chatId, 3);
    }
}
