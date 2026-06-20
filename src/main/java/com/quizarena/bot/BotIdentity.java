package com.quizarena.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class BotIdentity {

    private final TelegramClient telegramClient;
    private volatile String username;

    public BotIdentity(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    public String username() throws TelegramApiException {
        String cached = username;
        if (cached == null) {
            cached = telegramClient.execute(new GetMe()).getUserName();
            username = cached;
        }
        return cached;
    }
}
