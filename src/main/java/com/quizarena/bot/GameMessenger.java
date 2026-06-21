package com.quizarena.bot;

import com.quizarena.domain.GameResult;
import com.quizarena.domain.PersonalRank;
import com.quizarena.domain.Question;
import com.quizarena.domain.Standing;
import com.quizarena.handler.MessageBuilder;
import com.quizarena.render.RankCardRenderer;
import com.quizarena.render.ResultCardRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;

@Component
public class GameMessenger {

    private static final Logger log = LoggerFactory.getLogger(GameMessenger.class);
    private static final int CAPTION_LIMIT = 1024;
    private static final int CAPTION_TOP = 10;

    private final TelegramClient telegramClient;
    private final MessageBuilder messageBuilder;
    private final ResultCardRenderer resultCardRenderer;
    private final RankCardRenderer rankCardRenderer;

    public GameMessenger(TelegramClient telegramClient, MessageBuilder messageBuilder,
                         ResultCardRenderer resultCardRenderer, RankCardRenderer rankCardRenderer) {
        this.telegramClient = telegramClient;
        this.messageBuilder = messageBuilder;
        this.resultCardRenderer = resultCardRenderer;
        this.rankCardRenderer = rankCardRenderer;
    }

    public void notice(long chatId, String text) throws TelegramApiException {
        telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build());
    }

    public int sendLobby(long chatId, int seconds, Locale locale) throws TelegramApiException {
        Message sent = telegramClient.execute(SendMessage.builder()
                .chatId(chatId).text(messageBuilder.lobbyText(locale, 0, seconds))
                .replyMarkup(messageBuilder.joinKeyboard(locale)).parseMode("HTML").build());
        return sent.getMessageId();
    }

    public void updateLobby(long chatId, int messageId, int participants, int seconds, Locale locale)
            throws TelegramApiException {
        telegramClient.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(messageBuilder.lobbyText(locale, participants, seconds))
                .replyMarkup(messageBuilder.joinKeyboard(locale)).parseMode("HTML").build());
    }

    public void lobbyCancelled(long chatId, int messageId, Locale locale) throws TelegramApiException {
        editPlain(chatId, messageId, messageBuilder.lobbyCancelledText(locale));
    }

    public void lobbyStarted(long chatId, int messageId, int participants, Locale locale) throws TelegramApiException {
        editPlain(chatId, messageId, messageBuilder.lobbyStartedText(locale, participants));
    }

    public int sendQuestion(long chatId, Question question, int index, int total, long token, Locale locale)
            throws TelegramApiException {
        Message sent = telegramClient.execute(SendMessage.builder()
                .chatId(chatId).text(messageBuilder.questionText(locale, question, index, total))
                .replyMarkup(messageBuilder.answerKeyboard(question, token)).parseMode("HTML").build());
        return sent.getMessageId();
    }

    public void revealAnswer(long chatId, int messageId, Question question, Locale locale) throws TelegramApiException {
        telegramClient.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(messageBuilder.revealText(locale, question))
                .replyMarkup(InlineKeyboardMarkup.builder().build()).parseMode("HTML").build());
    }

    public void sendScoreboard(long chatId, List<Standing> standings, Locale locale) throws TelegramApiException {
        telegramClient.execute(SendMessage.builder()
                .chatId(chatId).text(messageBuilder.scoreboardText(locale, standings)).parseMode("HTML").build());
    }

    public void sendRank(long chatId, PersonalRank group, PersonalRank global, int elo, Locale locale)
            throws TelegramApiException {
        telegramClient.execute(SendMessage.builder()
                .chatId(chatId).text(messageBuilder.rankText(locale, group, global, elo)).parseMode("HTML").build());
    }

    public void sendRankCard(long chatId, String name, PersonalRank group, PersonalRank global, int elo,
                             byte[] avatar, Locale locale) {
        Thread.ofVirtual().name("rank-card").start(() -> {
            try {
                sendPhoto(chatId, rankCardRenderer.render(name, group, global, avatar, chatId > 0, locale),
                        messageBuilder.eloCaption(locale, elo),
                        chatId > 0 ? messageBuilder.rankNavKeyboard(locale) : null);
            } catch (Exception e) {
                log.warn("Rank card failed in chat {}, falling back to text", chatId, e);
                try {
                    sendRank(chatId, group, global, elo, locale);
                } catch (Exception fallback) {
                    log.error("Rank text fallback failed in chat {}", chatId, fallback);
                }
            }
        });
    }

    public void answerCallback(String callbackId, String text) throws TelegramApiException {
        telegramClient.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackId).text(text).build());
    }

    public void sendResult(long chatId, GameResult result, Locale locale) {
        Thread.ofVirtual().name("result-card").start(() -> {
            try {
                if (!result.hasWinner()) {
                    sendScoreboard(chatId, result.scoreboard(), locale);
                    return;
                }
                byte[] png = resultCardRenderer.render(result.categorySlug(), result.winnerName(),
                        result.winnerScore(), accuracy(result), result.correct(), result.globalPlace(),
                        result.winnerAvatar(), locale);
                if (!result.group()) {
                    sendPhoto(chatId, png, null);
                    return;
                }
                String caption = messageBuilder.scoreboardText(locale, topSlice(result.scoreboard()));
                if (caption.length() <= CAPTION_LIMIT) {
                    sendPhoto(chatId, png, caption);
                } else {
                    sendPhoto(chatId, png, null);
                    sendScoreboard(chatId, result.scoreboard(), locale);
                }
            } catch (Exception e) {
                log.warn("Result card failed in chat {}, falling back to text", chatId, e);
                try {
                    sendScoreboard(chatId, result.scoreboard(), locale);
                } catch (Exception fallback) {
                    log.error("Result text fallback failed in chat {}", chatId, fallback);
                }
            }
        });
    }

    private void sendPhoto(long chatId, byte[] png, String caption) throws TelegramApiException {
        InputFile photo = new InputFile(new ByteArrayInputStream(png), "card.png");
        SendPhoto message = caption == null
                ? SendPhoto.builder().chatId(chatId).photo(photo).build()
                : SendPhoto.builder().chatId(chatId).photo(photo).caption(caption).parseMode("HTML").build();
        telegramClient.execute(message);
    }

    private void sendPhoto(long chatId, byte[] png, String caption, InlineKeyboardMarkup markup)
            throws TelegramApiException {
        telegramClient.execute(SendPhoto.builder().chatId(chatId)
                .photo(new InputFile(new ByteArrayInputStream(png), "card.png"))
                .caption(caption).parseMode("HTML").replyMarkup(markup).build());
    }

    private void editPlain(long chatId, int messageId, String text) throws TelegramApiException {
        telegramClient.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(text).build());
    }

    private static int accuracy(GameResult result) {
        return result.answered() == 0 ? 0 : (int) Math.round(result.correct() * 100.0 / result.answered());
    }

    private static List<Standing> topSlice(List<Standing> standings) {
        return standings.size() > CAPTION_TOP ? standings.subList(0, CAPTION_TOP) : standings;
    }
}
