package com.quizarena.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "game")
public record GameProperties(
        int lobbySeconds,
        int questionSeconds,
        int basePoints,
        int questionsPerGame,
        Duration ttl) {
}
