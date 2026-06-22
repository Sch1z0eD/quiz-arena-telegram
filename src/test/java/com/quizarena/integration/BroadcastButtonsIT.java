package com.quizarena.integration;

import com.quizarena.domain.Broadcast;
import com.quizarena.domain.BroadcastButton;
import com.quizarena.repository.BroadcastRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// The button rows are stored as JSONB and mapped with @JdbcTypeCode(SqlTypes.JSON); this proves a real
// round-trip through Postgres keeps the nested List<List<BroadcastButton>> structure intact.
class BroadcastButtonsIT extends AbstractIntegrationTest {

    @Autowired
    private BroadcastRepository broadcasts;

    @Test
    void persistsAndReadsBackNestedButtonRows() {
        List<List<BroadcastButton>> rows = List.of(
                List.of(new BroadcastButton("A", "https://a"), new BroadcastButton("B", "https://b")),
                List.of(new BroadcastButton("C", "https://c")));
        long id = broadcasts.save(new Broadcast(1L, 0L, "all", null, "hi", null, rows, "DRAFT", 0, "tok-btn")).getId();

        List<List<BroadcastButton>> read = broadcasts.findById(id).orElseThrow().getButtons();

        assertEquals(2, read.size(), "row count survives");
        assertEquals(2, read.get(0).size());
        assertEquals(new BroadcastButton("A", "https://a"), read.get(0).get(0));
        assertEquals(new BroadcastButton("B", "https://b"), read.get(0).get(1));
        assertEquals(1, read.get(1).size());
        assertEquals(new BroadcastButton("C", "https://c"), read.get(1).get(0));
    }

    @Test
    void readsBackNullWhenNoButtons() {
        long id = broadcasts.save(new Broadcast(1L, 0L, "all", null, "hi", null, null, "DRAFT", 0, "tok-none")).getId();
        assertNull(broadcasts.findById(id).orElseThrow().getButtons());
    }
}
