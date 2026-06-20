package com.quizarena.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DuelStoreTest {

    @Test
    void queueKeyIsLanguageScoped() {
        assertEquals("mm:ru:science:easy", DuelStore.queueKey("ru", "science", "easy"));
        assertNotEquals(DuelStore.queueKey("en", "science", "easy"),
                DuelStore.queueKey("ru", "science", "easy"),
                "different languages must use different queues — no cross-language match");
    }
}
