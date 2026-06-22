package com.quizarena.integration;

import com.quizarena.domain.Broadcast;
import com.quizarena.repository.BroadcastRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BroadcastRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private BroadcastRepository broadcasts;

    @Test
    void startIfDraftFlipsExactlyOnce() {
        long id = save("all", "DRAFT", "tok-A", 0);

        assertEquals(1, broadcasts.startIfDraft(id, "tok-A", 50), "first flip with the right token wins");
        assertEquals("RUNNING", broadcasts.findById(id).orElseThrow().getStatus());
        assertEquals(50, broadcasts.findById(id).orElseThrow().getTotal(), "total pinned to live recipient count");

        assertEquals(0, broadcasts.startIfDraft(id, "tok-A", 50), "a second start cannot re-flip a RUNNING row");
    }

    @Test
    void startIfDraftRejectsWrongToken() {
        long id = save("all", "DRAFT", "tok-B", 0);
        assertEquals(0, broadcasts.startIfDraft(id, "wrong", 10), "wrong token does not start");
        assertEquals("DRAFT", broadcasts.findById(id).orElseThrow().getStatus());
    }

    @Test
    void markRunningInterruptedFlipsRunningRows() {
        long id = save("all", "RUNNING", null, 5);
        assertTrue(broadcasts.markRunningInterrupted() >= 1);
        assertEquals("INTERRUPTED", broadcasts.findById(id).orElseThrow().getStatus());
    }

    @Test
    void finishWritesStatusAndCounts() {
        long id = save("all", "RUNNING", null, 7);
        broadcasts.finish(id, "DONE", 5, 2);
        Broadcast done = broadcasts.findById(id).orElseThrow();
        assertEquals("DONE", done.getStatus());
        assertEquals(5, done.getSent());
        assertEquals(2, done.getFailed());
    }

    @Test
    void existsByStatusReflectsRunning() {
        save("all", "RUNNING", null, 1);
        assertTrue(broadcasts.existsByStatus("RUNNING"));
        assertFalse(broadcasts.existsByStatus("NOSUCH"));
    }

    private long save(String segment, String status, String token, int total) {
        Broadcast broadcast = new Broadcast(1L, System.currentTimeMillis(), segment, null, "hi",
                null, null, status, total, token);
        return broadcasts.save(broadcast).getId();
    }
}
