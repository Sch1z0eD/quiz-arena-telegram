package com.quizarena.handler;

import com.quizarena.bot.GameMessenger;
import com.quizarena.service.LocaleService;
import com.quizarena.service.MenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.Locale;

@Component
public class MenuCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(MenuCallbackHandler.class);

    private final MenuService menuService;
    private final GameMessenger messenger;
    private final MessageBuilder messageBuilder;
    private final LocaleService localeService;

    public MenuCallbackHandler(MenuService menuService, GameMessenger messenger, MessageBuilder messageBuilder,
                               LocaleService localeService) {
        this.menuService = menuService;
        this.messenger = messenger;
        this.messageBuilder = messageBuilder;
        this.localeService = localeService;
    }

    public void handle(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        int messageId = callbackQuery.getMessage().getMessageId();
        Locale locale = localeService.resolve(callbackQuery.getFrom().getId(), callbackQuery.getFrom().getLanguageCode());
        String toast = "";
        try {
            toast = menuService.navigate(chatId, messageId, callbackQuery.getFrom().getId(),
                    TelegramNames.displayName(callbackQuery.getFrom()), callbackQuery.getData(), locale);
        } catch (Exception e) {
            log.error("Failed to handle menu callback in chat {}", chatId, e);
            toast = messageBuilder.errorGeneric(locale);
        }
        try {
            messenger.answerCallback(callbackQuery.getId(), toast);
        } catch (Exception e) {
            log.error("Failed to answer menu callback {}", callbackQuery.getId(), e);
        }
    }
}
