package com.quizarena.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quiz.import")
public record ImportProperties(boolean enabled, int amountPerCall) {
}
