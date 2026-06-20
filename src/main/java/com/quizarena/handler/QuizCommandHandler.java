package com.quizarena.handler;

import com.quizarena.domain.Category;
import com.quizarena.domain.Difficulty;
import com.quizarena.domain.TopScope;
import com.quizarena.service.DuelService;
import com.quizarena.service.GameService;
import com.quizarena.service.LocaleService;
import com.quizarena.service.MenuService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Locale;

@Component
public class QuizCommandHandler {

    private final GameService gameService;
    private final MenuService menuService;
    private final LocaleService localeService;
    private final DuelService duelService;

    public QuizCommandHandler(GameService gameService, MenuService menuService, LocaleService localeService,
                              DuelService duelService) {
        this.gameService = gameService;
        this.menuService = menuService;
        this.localeService = localeService;
        this.duelService = duelService;
    }

    public void handle(Message message) throws TelegramApiException {
        String[] parts = message.getText().trim().split("\\s+");
        String command = parts[0];
        long chatId = message.getChatId();
        long userId = message.getFrom().getId();
        Locale locale = localeService.resolve(userId, message.getFrom().getLanguageCode());

        if (command.startsWith("/start") && parts.length > 1 && parts[1].startsWith("duel_")) {
            boolean started = duelService.acceptInvite(parts[1].substring("duel_".length()), userId, chatId,
                    TelegramNames.displayName(message.getFrom()), locale);
            if (!started) {
                menuService.openMenu(chatId, chatId > 0, locale);
            }
        } else if (command.startsWith("/start") && parts.length > 1 && parts[1].equals("play")) {
            menuService.openPicker(chatId, locale);
        } else if (command.startsWith("/start") || command.startsWith("/menu")) {
            menuService.openMenu(chatId, chatId > 0, locale);
        } else if (command.startsWith("/quiz")) {
            String[] filter = parseFilter(parts);
            if (filter[0].isEmpty() && filter[1].isEmpty()) {
                menuService.openPicker(chatId, locale);
            } else {
                gameService.startQuiz(chatId, chatId < 0, userId,
                        TelegramNames.displayName(message.getFrom()), filter[0], filter[1], locale);
            }
        } else if (command.startsWith("/top")) {
            menuService.openLeaderboard(chatId, parseScope(parts), userId, locale);
        } else if (command.startsWith("/rank")) {
            gameService.showRank(chatId, userId, TelegramNames.displayName(message.getFrom()), locale);
        }
    }

    private static String[] parseFilter(String[] parts) {
        String category = "";
        String difficulty = "";
        for (int i = 1; i < parts.length; i++) {
            String token = parts[i].toLowerCase();
            if (Difficulty.fromValue(token) != null) {
                difficulty = token;
            } else {
                Category matched = Category.fromSlug(token);
                if (matched != null) {
                    category = matched.slug();
                }
            }
        }
        return new String[]{category, difficulty};
    }

    private static TopScope parseScope(String[] parts) {
        if (parts.length > 1) {
            String token = parts[1].toLowerCase();
            if ("global".equals(token)) {
                return TopScope.GLOBAL;
            }
            if ("week".equals(token)) {
                return TopScope.WEEK;
            }
        }
        return TopScope.GROUP;
    }
}
