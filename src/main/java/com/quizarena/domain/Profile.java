package com.quizarena.domain;

public record Profile(long games, long answered, long correct, int accuracyPercent, long points, Long place, int elo) {}
