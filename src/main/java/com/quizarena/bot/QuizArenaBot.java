package com.quizarena.bot;

import com.quizarena.config.TelegramProperties;
import com.quizarena.handler.DuelCallbackHandler;
import com.quizarena.handler.InlineQueryHandler;
import com.quizarena.handler.MenuCallbackHandler;
import com.quizarena.handler.QuizCallbackHandler;
import com.quizarena.handler.QuizCommandHandler;
import com.quizarena.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class QuizArenaBot implements SpringLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(QuizArenaBot.class);

    private final String token;
    private final QuizCommandHandler commandHandler;
    private final QuizCallbackHandler callbackHandler;
    private final MenuCallbackHandler menuCallbackHandler;
    private final DuelCallbackHandler duelCallbackHandler;
    private final InlineQueryHandler inlineQueryHandler;
    private final UserService userService;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public QuizArenaBot(TelegramProperties properties,
                        QuizCommandHandler commandHandler,
                        QuizCallbackHandler callbackHandler,
                        MenuCallbackHandler menuCallbackHandler,
                        DuelCallbackHandler duelCallbackHandler,
                        InlineQueryHandler inlineQueryHandler,
                        UserService userService) {
        this.token = properties.token();
        this.commandHandler = commandHandler;
        this.callbackHandler = callbackHandler;
        this.menuCallbackHandler = menuCallbackHandler;
        this.duelCallbackHandler = duelCallbackHandler;
        this.inlineQueryHandler = inlineQueryHandler;
        this.userService = userService;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updates -> updates.forEach(update -> executor.submit(() -> dispatch(update)));
    }

    private void dispatch(Update update) {
        try {
            userService.touch(from(update));
            if (update.hasMessage() && update.getMessage().hasText()) {
                commandHandler.handle(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                var callback = update.getCallbackQuery();
                String data = callback.getData();
                if (data != null && data.startsWith("m:")) {
                    menuCallbackHandler.handle(callback);
                } else if (data != null && data.startsWith("d:")) {
                    duelCallbackHandler.handleAnswer(callback);
                } else {
                    callbackHandler.handleAnswer(callback);
                }
            } else if (update.hasInlineQuery()) {
                inlineQueryHandler.handle(update.getInlineQuery());
            }

        } catch (Exception e) {
            log.error("Failed to process update {}", update.getUpdateId(), e);
        }
    }

    private static User from(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getFrom();
        }
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom();
        }
        if (update.hasInlineQuery()) {
            return update.getInlineQuery().getFrom();
        }
        return null;
    }
}
