package com.quizarena.service;

public final class Scoring {

    private Scoring() {
    }

    public static long speedBonus(long questionStartMillis, long now, long totalMillis, int basePoints) {
        long remaining = Math.max(0L, totalMillis - (now - questionStartMillis));
        return (long) basePoints * remaining / totalMillis;
    }
}
