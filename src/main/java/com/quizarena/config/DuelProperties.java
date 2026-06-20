package com.quizarena.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "duel")
public record DuelProperties(int searchSeconds, int questionSeconds, int questionCount, int basePoints) {
}
