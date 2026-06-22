package com.quizarena.service;

import com.quizarena.domain.Broadcast;
import com.quizarena.domain.BroadcastButton;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BroadcastSenderTest {

    private final TelegramClient client = mock(TelegramClient.class);
    private final BroadcastSender sender = new BroadcastSender(client);

    @Test
    void buildsInlineKeyboardPreservingRowsAndButtons() throws Exception {
        List<List<BroadcastButton>> rows = List.of(
                List.of(new BroadcastButton("A", "https://a"), new BroadcastButton("B", "https://b")),
                List.of(new BroadcastButton("C", "https://c")));

        sender.send(42L, broadcast(rows));

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(client).execute(captor.capture());
        InlineKeyboardMarkup markup = assertInstanceOf(InlineKeyboardMarkup.class, captor.getValue().getReplyMarkup());

        List<InlineKeyboardRow> keyboard = markup.getKeyboard();
        assertEquals(2, keyboard.size());
        assertEquals(2, keyboard.get(0).size());
        assertEquals("A", keyboard.get(0).get(0).getText());
        assertEquals("https://a", keyboard.get(0).get(0).getUrl());
        assertEquals("https://b", keyboard.get(0).get(1).getUrl());
        assertEquals(1, keyboard.get(1).size());
        assertEquals("C", keyboard.get(1).get(0).getText());
    }

    @Test
    void omitsKeyboardWhenNoButtons() throws Exception {
        sender.send(42L, broadcast(null));

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(client).execute(captor.capture());
        assertNull(captor.getValue().getReplyMarkup());
    }

    @Test
    void sendsPhotoByFileIdWithoutReuploading() throws Exception {
        String fileId = "AgACAgIAAxkBAap-file-id";
        Broadcast broadcast = new Broadcast(1L, 0L, "all", null, "hello", fileId, null, "RUNNING", 0, null);

        sender.send(7L, broadcast);

        ArgumentCaptor<SendPhoto> captor = ArgumentCaptor.forClass(SendPhoto.class);
        verify(client).execute(captor.capture());
        InputFile photo = captor.getValue().getPhoto();
        assertEquals(fileId, photo.getAttachName());
        assertFalse(photo.isNew(), "reuses the file_id instead of re-uploading bytes");
        assertNull(photo.getNewMediaStream());
    }

    private static Broadcast broadcast(List<List<BroadcastButton>> buttons) {
        return new Broadcast(1L, 0L, "all", null, "hello", null, buttons, "RUNNING", 0, null);
    }
}
