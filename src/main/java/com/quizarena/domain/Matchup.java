package com.quizarena.domain;

public record Matchup(String nameA, int eloA, byte[] avatarA,
                      String nameB, int eloB, byte[] avatarB,
                      String category, String difficulty) {
}
