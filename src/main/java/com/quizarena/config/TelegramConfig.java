package com.quizarena.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class TelegramConfig {

    @Bean
    public TelegramClient telegramClient(TelegramProperties properties) {
        return new OkHttpTelegramClient(properties.token());
    }
}
