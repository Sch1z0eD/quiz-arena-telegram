package com.quizarena.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brand")
public record BrandProperties(String name, String handle) {
}
