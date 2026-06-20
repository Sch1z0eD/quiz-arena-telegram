package com.quizarena.handler;

import com.quizarena.bot.DuelMessenger;
import com.quizarena.domain.RecordResult;
import com.quizarena.service.DuelService;
import com.quizarena.service.LocaleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.Locale;

@Component
public class DuelCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(DuelCallbackHandler.class);

    private final DuelService duelService;
    private final DuelMessenger messenger;
    private final MessageBuilder messageBuilder;
    private final LocaleService localeService;

    public DuelCallbackHandler(DuelService duelService, DuelMessenger messenger, MessageBuilder messageBuilder,
                               LocaleService localeService) {
        this.duelService = duelService;
        this.messenger = messenger;
        this.messageBuilder = messageBuilder;
        this.localeService = localeService;
    }

    public void handleAnswer(CallbackQuery callbackQuery) {
        long userId = callbackQuery.getFrom().getId();
        String data = callbackQuery.getData();
        Locale locale = localeService.resolve(userId, callbackQuery.getFrom().getLanguageCode());
        String toast = "";
        Long finishDuelId = null;
        long finishToken = 0L;
        try {
            String[] parts = data.split(":");
            long duelId = Long.parseLong(parts[1]);
            long token = Long.parseLong(parts[2]);
            int option = Integer.parseInt(parts[3]);
            RecordResult result = duelService.recordAnswer(duelId, userId, token, option);
            toast = messageBuilder.answerToast(locale, result);
            if (result.allAnswered()) {
                finishDuelId = duelId;
                finishToken = token;
            }
        } catch (Exception e) {
            log.error("Failed to handle duel callback", e);
            toast = messageBuilder.errorGeneric(locale);
        }
        messenger.answerCallback(callbackQuery.getId(), toast);
        if (finishDuelId != null) {
            try {
                duelService.finishQuestion(finishDuelId, finishToken);
            } catch (Exception e) {
                log.error("Failed to finish duel question {}", finishDuelId, e);
            }
        }
    }
}
