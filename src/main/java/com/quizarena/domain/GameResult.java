package com.quizarena.domain;

import java.util.List;

public record GameResult(
        String categorySlug,
        boolean hasWinner,
        String winnerName,
        long winnerScore,
        long correct,
        long answered,
        Long globalPlace,
        List<Standing> scoreboard,
        boolean group,
        byte[] winnerAvatar) {
}
