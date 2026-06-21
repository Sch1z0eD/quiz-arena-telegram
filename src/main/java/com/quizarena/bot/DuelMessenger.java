package com.quizarena.bot;

import com.quizarena.domain.DuelResult;
import com.quizarena.domain.Matchup;
import com.quizarena.domain.OptionOrder;
import com.quizarena.domain.Question;
import com.quizarena.handler.MessageBuilder;
import com.quizarena.handler.UiTexts;
import com.quizarena.render.DuelMatchupCardRenderer;
import com.quizarena.render.DuelResultCardRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;

@Component
public class DuelMessenger {

    private static final Logger log = LoggerFactory.getLogger(DuelMessenger.class);

    private final TelegramClient telegramClient;
    private final MessageBuilder messageBuilder;
    private final UiTexts texts;
    private final DuelResultCardRenderer duelResultCardRenderer;
    private final DuelMatchupCardRenderer duelMatchupCardRenderer;

    public DuelMessenger(TelegramClient telegramClient, MessageBuilder messageBuilder, UiTexts texts,
                         DuelResultCardRenderer duelResultCardRenderer,
                         DuelMatchupCardRenderer duelMatchupCardRenderer) {
        this.telegramClient = telegramClient;
        this.messageBuilder = messageBuilder;
        this.texts = texts;
        this.duelResultCardRenderer = duelResultCardRenderer;
        this.duelMatchupCardRenderer = duelMatchupCardRenderer;
    }

    public void editSearching(long chatId, int messageId, String bucketCategory, String bucketDifficulty,
                              Locale locale) {
        InlineKeyboardMarkup cancel = InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text(texts.btnCancelSearch(locale))
                        .callbackData("m:cancel:" + locale.getLanguage() + ":" + bucketCategory + ":" + bucketDifficulty)
                        .build()))).build();
        editStatus(chatId, messageId, texts.duelSearching(locale), cancel);
    }

    public void editBusy(long chatId, int messageId, Locale locale) {
        editStatus(chatId, messageId, texts.duelBusy(locale), noKeyboard());
    }

    public void editFound(long chatId, int messageId, Locale locale) {
        editStatus(chatId, messageId, texts.duelSearchFound(locale), noKeyboard());
    }

    public void editFailed(long chatId, int messageId, Locale locale) {
        editStatus(chatId, messageId, texts.duelSearchFailed(locale), noKeyboard());
    }

    public void editCancelled(long chatId, int messageId, Locale locale) {
        InlineKeyboardMarkup menu = InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text(texts.btnMainMenu(locale))
                        .callbackData("m:home")
                        .build()))).build();
        editStatus(chatId, messageId, texts.duelSearchCancelled(locale), menu);
    }

    // Matchup card is cosmetic: on any render/send failure fall back to the plain "opponent found" text
    // so the duel is never blocked by the card. Random path edits the existing search banner's media;
    // the invite path has no banner, so it sends a fresh photo.
    public void editMatchup(long chatId, int messageId, Matchup matchup, Locale locale) {
        try {
            byte[] png = duelMatchupCardRenderer.render(matchup, locale);
            InputMediaPhoto media = InputMediaPhoto.builder()
                    .media(new ByteArrayInputStream(png), "matchup.png").build();
            telegramClient.execute(EditMessageMedia.builder()
                    .chatId(chatId).messageId(messageId).media(media).replyMarkup(noKeyboard()).build());
        } catch (Exception e) {
            log.warn("Matchup card (edit) failed in chat {}, falling back to text", chatId, e);
            editFound(chatId, messageId, locale);
        }
    }

    public void sendMatchup(long chatId, Matchup matchup, Locale locale) {
        try {
            byte[] png = duelMatchupCardRenderer.render(matchup, locale);
            telegramClient.execute(SendPhoto.builder().chatId(chatId)
                    .photo(new InputFile(new ByteArrayInputStream(png), "matchup.png")).build());
        } catch (Exception e) {
            log.warn("Matchup card (send) failed in chat {}, falling back to text", chatId, e);
            notify(chatId, texts.duelSearchFound(locale));
        }
    }

    public int sendQuestion(long chatId, Question question, OptionOrder order, int index, int total, long duelId, long token,
                            Locale locale) throws TelegramApiException {
        Message sent = telegramClient.execute(SendMessage.builder()
                .chatId(chatId).text(messageBuilder.questionText(locale, question, index, total))
                .replyMarkup(messageBuilder.duelAnswerKeyboard(question, order, duelId, token)).parseMode("HTML").build());
        return sent.getMessageId();
    }

    public void reveal(long chatId, int messageId, Question question, OptionOrder order, Locale locale) {
        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId).text(messageBuilder.revealText(locale, question, order))
                    .replyMarkup(noKeyboard()).parseMode("HTML").build());
        } catch (TelegramApiException e) {
            log.warn("Duel reveal failed in chat {}", chatId, e);
        }
    }

    public void sendResult(long chatA, Locale localeA, long chatB, Locale localeB, DuelResult result) {
        sendResultTo(chatA, localeA, result);
        sendResultTo(chatB, localeB, result);
    }

    public void answerCallback(String callbackId, String text) {
        try {
            telegramClient.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackId).text(text).build());
        } catch (Exception e) {
            log.warn("Duel answerCallback failed {}", callbackId, e);
        }
    }

    public void notify(long chatId, String text) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (TelegramApiException e) {
            log.warn("Duel notify failed in chat {}", chatId, e);
        }
    }

    private void sendResultTo(long chatId, Locale locale, DuelResult result) {
        try {
            byte[] png = duelResultCardRenderer.render(result, locale);
            telegramClient.execute(SendPhoto.builder().chatId(chatId)
                    .photo(new InputFile(new ByteArrayInputStream(png), "duel.png"))
                    .caption(messageBuilder.duelEloCaption(locale, result)).parseMode("HTML").build());
        } catch (Exception e) {
            log.warn("Duel result card failed in chat {}, falling back to text", chatId, e);
            try {
                telegramClient.execute(SendMessage.builder()
                        .chatId(chatId).text(messageBuilder.duelResultText(locale, result)).parseMode("HTML").build());
            } catch (Exception fallback) {
                log.error("Duel result text fallback failed in chat {}", chatId, fallback);
            }
        }
    }

    // The duel category/difficulty screen is a banner photo, so search statuses edit its caption.
    private void editStatus(long chatId, int messageId, String caption, InlineKeyboardMarkup markup) {
        try {
            telegramClient.execute(EditMessageCaption.builder()
                    .chatId(chatId).messageId(messageId).caption(caption).replyMarkup(markup).parseMode("HTML").build());
        } catch (TelegramApiException e) {
            log.warn("Duel status edit failed in chat {}", chatId, e);
        }
    }

    private static InlineKeyboardMarkup noKeyboard() {
        return InlineKeyboardMarkup.builder().build();
    }
}
