package com.quizarena.domain;

public record DuelResult(String categorySlug, String nameA, long scoreA, String nameB, long scoreB, Outcome outcome,
                         int eloA, int eloDeltaA, int eloB, int eloDeltaB) {

    public enum Outcome {
        A_WINS,
        B_WINS,
        DRAW
    }
}
