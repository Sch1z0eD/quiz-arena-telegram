package com.quizarena.handler;

import com.quizarena.bot.GameMessenger;
import com.quizarena.domain.JoinResult;
import com.quizarena.domain.RecordResult;
import com.quizarena.service.GameService;
import com.quizarena.service.LocaleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.Locale;

@Component
public class QuizCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(QuizCallbackHandler.class);

    private final GameService gameService;
    private final GameMessenger messenger;
    private final MessageBuilder messageBuilder;
    private final LocaleService localeService;

    public QuizCallbackHandler(GameService gameService, GameMessenger messenger, MessageBuilder messageBuilder,
                               LocaleService localeService) {
        this.gameService = gameService;
        this.messenger = messenger;
        this.messageBuilder = messageBuilder;
        this.localeService = localeService;
    }

    public void handleAnswer(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();
        Locale locale = localeService.resolve(callbackQuery.getFrom().getId(), callbackQuery.getFrom().getLanguageCode());

        String toast = "";
        Long finishToken = null;
        try {
            if ("join".equals(data)) {
                JoinResult result = gameService.join(chatId, callbackQuery.getFrom().getId(),
                        TelegramNames.displayName(callbackQuery.getFrom()));
                toast = messageBuilder.joinToast(locale, result);
            } else if (data != null && data.startsWith("a:")) {
                String[] parts = data.split(":");
                long token = Long.parseLong(parts[1]);
                int option = Integer.parseInt(parts[2]);
                RecordResult result = gameService.recordAnswer(chatId, callbackQuery.getFrom().getId(), token, option);
                toast = messageBuilder.answerToast(locale, result);
                if (result.allAnswered()) {
                    finishToken = token;
                }
            }
        } catch (Exception e) {
            log.error("Failed to handle callback in chat {}", chatId, e);
            toast = messageBuilder.errorGeneric(locale);
        }

        answer(callbackQuery.getId(), toast);

        if (finishToken != null) {
            try {
                gameService.finishQuestion(chatId, finishToken);
            } catch (Exception e) {
                log.error("Failed to finish question in chat {}", chatId, e);
            }
        }
    }

    private void answer(String callbackId, String text) {
        try {
            messenger.answerCallback(callbackId, text);
        } catch (Exception e) {
            log.error("Failed to answer callback {}", callbackId, e);
        }
    }
}
