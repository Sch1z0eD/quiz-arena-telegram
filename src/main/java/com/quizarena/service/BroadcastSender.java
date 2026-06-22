package com.quizarena.service;

import com.quizarena.domain.Broadcast;
import com.quizarena.domain.BroadcastButton;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    // Uploads the image once and returns the Telegram file_id so the broadcast can reuse it for every recipient
    // instead of re-uploading the bytes per send. The photo lands in the uploader's own chat as a side effect.
    public String uploadPhoto(long chatId, byte[] bytes, String filename) throws TelegramApiException {
        Message message = telegramClient.execute(SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(new ByteArrayInputStream(bytes), filename))
                .build());
        return message.getPhoto().stream()
                .max(Comparator.comparingInt(PhotoSize::getWidth))
                .orElseThrow(() -> new IllegalStateException("Telegram returned no photo sizes"))
                .getFileId();
    }

    private static InlineKeyboardMarkup keyboard(Broadcast broadcast) {
        List<List<BroadcastButton>> rows = broadcast.getButtons();
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        for (List<BroadcastButton> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            InlineKeyboardRow keyboardRow = new InlineKeyboardRow();
            for (BroadcastButton button : row) {
                keyboardRow.add(InlineKeyboardButton.builder().text(button.text()).url(button.url()).build());
            }
            keyboard.add(keyboardRow);
        }
        return keyboard.isEmpty() ? null : InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}
