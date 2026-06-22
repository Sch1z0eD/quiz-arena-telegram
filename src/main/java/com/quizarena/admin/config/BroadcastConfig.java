package com.quizarena.admin.config;

import com.quizarena.service.BroadcastSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class BroadcastConfig {

    @Bean
    public BroadcastSender broadcastSender(TelegramClient telegramClient) {
        return new BroadcastSender(telegramClient);
    }

    @Bean
    public Executor broadcastExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
