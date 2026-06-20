package com.quizarena.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Component
public class InlineMessenger {

    private static final Logger log = LoggerFactory.getLogger(InlineMessenger.class);

    private final TelegramClient telegramClient;

    public InlineMessenger(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    public void answer(String inlineQueryId, List<InlineQueryResult> results) {
        AnswerInlineQuery answer = new AnswerInlineQuery(inlineQueryId, results);
        answer.setCacheTime(0);
        answer.setIsPersonal(true);
        try {
            telegramClient.execute(answer);
        } catch (TelegramApiException e) {
            log.warn("answerInlineQuery failed for {}", inlineQueryId, e);
        }
    }
}
