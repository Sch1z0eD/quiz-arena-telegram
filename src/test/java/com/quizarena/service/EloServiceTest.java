package com.quizarena.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EloServiceTest {

    @Test
    void equalRatingsWinGainsHalfK() {
        assertEquals(1016, EloService.nextRating(1000, 1000, 1.0));
        assertEquals(984, EloService.nextRating(1000, 1000, 0.0));
    }

    @Test
    void equalRatingsDrawKeepsRating() {
        assertEquals(1000, EloService.nextRating(1000, 1000, 0.5));
    }

    @Test
    void underdogWinGainsMoreThanFavourite() {
        int underdogGain = EloService.nextRating(1000, 1400, 1.0) - 1000;
        int favouriteGain = EloService.nextRating(1400, 1000, 1.0) - 1400;
        assertTrue(underdogGain > favouriteGain, "underdog must gain more: " + underdogGain + " vs " + favouriteGain);
    }

    @Test
    void deltasAreApproximatelyZeroSum() {
        int a = 1200;
        int b = 1000;
        int deltaA = EloService.nextRating(a, b, 1.0) - a;
        int deltaB = EloService.nextRating(b, a, 0.0) - b;
        assertTrue(Math.abs(deltaA + deltaB) <= 1, "deltas should offset: " + deltaA + " / " + deltaB);
    }
}
