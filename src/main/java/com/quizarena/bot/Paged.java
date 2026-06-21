package com.quizarena.bot;

import java.util.List;

public record Paged<T>(List<T> items, int page, boolean hasPrev, boolean hasNext) {

    public static <T> Paged<T> of(List<T> all, int page, int size) {
        int totalPages = Math.max(1, (all.size() + size - 1) / size);
        int clamped = Math.min(Math.max(page, 0), totalPages - 1);
        int from = clamped * size;
        int to = Math.min(from + size, all.size());
        return new Paged<>(all.subList(from, to), clamped, clamped > 0, to < all.size());
    }
}
