package com.quizarena.service;

public record GameSettingsSnapshot(
        int questionsPerGame, int questionSeconds, int basePoints, int lobbySeconds,
        int duelSearchSeconds, int duelQuestionSeconds, int duelQuestionCount, int duelBasePoints) {
}
