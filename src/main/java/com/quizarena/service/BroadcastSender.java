package com.quizarena.service;

import com.quizarena.domain.Broadcast;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class BroadcastSender {

    private final TelegramClient telegramClient;

    public BroadcastSender(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    public void send(long chatId, Broadcast broadcast) throws TelegramApiException {
        InlineKeyboardMarkup markup = keyboard(broadcast);
        if (broadcast.getPhotoUrl() != null && !broadcast.getPhotoUrl().isBlank()) {
            telegramClient.execute(SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(broadcast.getPhotoUrl()))
                    .caption(broadcast.getText())
                    .parseMode("HTML")
                    .replyMarkup(markup)
                    .build());
        } else {
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(broadcast.getText())
                    .parseMode("HTML")
                    .replyMarkup(markup)
                    .build());
        }
    }

    private static InlineKeyboardMarkup keyboard(Broadcast broadcast) {
        if (broadcast.getButtonText() == null || broadcast.getButtonText().isBlank()) {
            return null;
        }
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text(broadcast.getButtonText())
                        .url(broadcast.getButtonUrl())
                        .build()))
                .build();
    }
}
