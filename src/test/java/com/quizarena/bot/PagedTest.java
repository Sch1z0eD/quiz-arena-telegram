package com.quizarena.bot;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagedTest {

    private static final List<String> TWENTY = IntStream.range(0, 20).mapToObj(i -> "c" + i).toList();

    @Test
    void firstPageHasNextButNoPrev() {
        Paged<String> page = Paged.of(TWENTY, 0, 8);
        assertEquals(8, page.items().size());
        assertEquals("c0", page.items().get(0));
        assertFalse(page.hasPrev());
        assertTrue(page.hasNext());
    }

    @Test
    void middlePageHasBothArrows() {
        Paged<String> page = Paged.of(TWENTY, 1, 8);
        assertEquals(8, page.items().size());
        assertEquals("c8", page.items().get(0));
        assertTrue(page.hasPrev());
        assertTrue(page.hasNext());
    }

    @Test
    void lastPageHasPrevButNoNext() {
        Paged<String> page = Paged.of(TWENTY, 2, 8);
        assertEquals(4, page.items().size());
        assertEquals("c16", page.items().get(0));
        assertTrue(page.hasPrev());
        assertFalse(page.hasNext());
    }

    @Test
    void singlePageHasNoArrows() {
        Paged<String> page = Paged.of(List.of("a", "b", "c"), 0, 8);
        assertEquals(3, page.items().size());
        assertFalse(page.hasPrev());
        assertFalse(page.hasNext());
    }

    @Test
    void outOfRangePageClampsToLast() {
        Paged<String> page = Paged.of(TWENTY, 99, 8);
        assertEquals(2, page.page());
        assertEquals(4, page.items().size());
        assertFalse(page.hasNext());
    }
}
