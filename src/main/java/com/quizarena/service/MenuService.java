package com.quizarena.service;

import com.quizarena.bot.MenuMessenger;
import com.quizarena.domain.TopScope;
import com.quizarena.handler.UiTexts;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class MenuService {

    // Reachable in a group: info cards plus the /quiz game picker. The full menu (home),
    // duels and language stay private-only.
    private static final Set<String> GROUP_ALLOWED =
            Set.of("profile", "board", "rank", "lb", "rules", "menu", "play", "cat", "diff");

    private final GameService gameService;
    private final MenuMessenger menuMessenger;
    private final UiTexts texts;
    private final DuelService duelService;
    private final LocaleService localeService;
    private final AvatarService avatarService;
    private final CategoryService categoryService;

    public MenuService(GameService gameService, MenuMessenger menuMessenger, UiTexts texts, DuelService duelService,
                       LocaleService localeService, AvatarService avatarService, CategoryService categoryService) {
        this.gameService = gameService;
        this.menuMessenger = menuMessenger;
        this.texts = texts;
        this.duelService = duelService;
        this.localeService = localeService;
        this.avatarService = avatarService;
        this.categoryService = categoryService;
    }

    public void openMenu(long chatId, boolean privateChat, Locale locale) throws TelegramApiException {
        if (privateChat) {
            menuMessenger.sendMainMenu(chatId, locale);
        } else {
            menuMessenger.hint(chatId, texts.groupHint(locale));
        }
    }

    public void openPicker(long chatId, Locale locale) throws TelegramApiException {
        if (!gameService.hasEnoughQuestions("", "", locale.getLanguage())) {
            menuMessenger.hint(chatId, texts.notEnoughQuestions(locale));
            return;
        }
        menuMessenger.sendCategories(chatId, gameService.availableCategories(locale.getLanguage()), chatId > 0, locale);
    }

    public void openLeaderboard(long chatId, TopScope scope, long userId, Locale locale) {
        var data = gameService.topData(scope, chatId, userId);
        menuMessenger.sendLeaderboardCard(chatId, scope, data.top(), data.personal(), locale);
    }

    public String navigate(long chatId, int messageId, long userId, String name, String data, Locale locale)
            throws TelegramApiException {
        String[] parts = data.split(":");
        String screen = parts.length > 1 ? parts[1] : "";
        if (chatId < 0 && !GROUP_ALLOWED.contains(screen)) {
            return texts.privateOnly(locale);
        }
        switch (screen) {
            case "home" -> menuMessenger.editMainMenu(chatId, messageId, locale);
            case "play" -> menuMessenger.editCategories(chatId, messageId,
                    gameService.availableCategories(locale.getLanguage()), chatId > 0, locale);
            case "cat" -> {
                String slug = parts.length > 2 ? parts[2] : "any";
                menuMessenger.editDifficulties(chatId, messageId, slug, categoryLabel(slug, locale), locale);
            }
            case "diff" -> {
                String slug = parts.length > 2 ? parts[2] : "any";
                String difficulty = parts.length > 3 ? parts[3] : "any";
                return startGame(chatId, messageId, slug, difficulty, userId, name, locale);
            }
            case "profile" -> {
                menuMessenger.deleteMessage(chatId, messageId);
                menuMessenger.sendProfileCard(chatId, gameService.profile(chatId, userId), name,
                        avatarService.get(userId), locale);
            }
            case "board" -> {
                menuMessenger.deleteMessage(chatId, messageId);
                openLeaderboard(chatId, TopScope.GROUP, userId, locale);
            }
            case "rank" -> {
                menuMessenger.deleteMessage(chatId, messageId);
                gameService.showRank(chatId, userId, name, locale);
            }
            case "menu" -> {
                menuMessenger.deleteMessage(chatId, messageId);
                if (chatId > 0) {
                    menuMessenger.sendMainMenu(chatId, locale);
                }
            }
            case "lb" -> {
                TopScope scope = parseScope(parts.length > 2 ? parts[2] : "");
                var ranking = gameService.topData(scope, chatId, userId);
                menuMessenger.editLeaderboardCard(chatId, messageId, scope, ranking.top(), ranking.personal(), locale);
            }
            case "rules" -> {
                menuMessenger.deleteMessage(chatId, messageId);
                menuMessenger.sendRules(chatId, locale);
            }
            case "lang" -> menuMessenger.editLanguageMenu(chatId, messageId, locale);
            case "setlang" -> {
                String language = parts.length > 2 ? parts[2] : "en";
                localeService.setLanguage(userId, language);
                menuMessenger.editMainMenu(chatId, messageId, localeService.parse(language));
            }
            case "duel" -> menuMessenger.editDuelCategories(chatId, messageId,
                    gameService.availableCategories(locale.getLanguage()), chatId > 0, locale);
            case "dcat" -> {
                String slug = parts.length > 2 ? parts[2] : "any";
                menuMessenger.editDuelDifficulties(chatId, messageId, slug, categoryLabel(slug, locale), locale);
            }
            case "ddiff" -> {
                String slug = parts.length > 2 ? parts[2] : "any";
                String difficulty = parts.length > 3 ? parts[3] : "any";
                menuMessenger.editDuelMode(chatId, messageId, slug, difficulty, locale);
            }
            case "dsearch" -> {
                String slug = parts.length > 2 ? parts[2] : "any";
                String difficulty = parts.length > 3 ? parts[3] : "any";
                duelService.search(chatId, userId, name,
                        "any".equals(slug) ? "" : slug, "any".equals(difficulty) ? "" : difficulty, messageId, locale);
            }
            case "dinvite" -> {
                String slug = parts.length > 2 ? parts[2] : "any";
                String difficulty = parts.length > 3 ? parts[3] : "any";
                Optional<DuelService.Invitation> invitation =
                        duelService.createInvite(userId, chatId, name, slug, difficulty, locale);
                if (invitation.isPresent()) {
                    menuMessenger.editDuelInvite(chatId, messageId,
                            invitation.get().link(), invitation.get().token(), locale);
                } else {
                    return texts.gameAlreadyRunning(locale);
                }
            }
            case "dcancelinvite" -> {
                duelService.cancelInvite(parts.length > 2 ? parts[2] : "");
                menuMessenger.editMainMenu(chatId, messageId, locale);
            }
            case "cancel" -> duelService.cancelSearch(chatId, userId, name,
                    parts.length > 2 ? parts[2] : locale.getLanguage(),
                    parts.length > 3 ? parts[3] : "any", parts.length > 4 ? parts[4] : "any", messageId, locale);
            default -> { }
        }
        return "";
    }

    private String startGame(long chatId, int messageId, String slug, String difficultyToken, long userId,
                             String name, Locale locale) throws TelegramApiException {
        String category = "any".equals(slug) ? "" : slug;
        String difficulty = "any".equals(difficultyToken) ? "" : difficultyToken;
        if (!gameService.hasEnoughQuestions(category, difficulty, locale.getLanguage())) {
            menuMessenger.editDifficulties(chatId, messageId, slug, categoryLabel(slug, locale), locale);
            return texts.notEnoughQuestions(locale);
        }
        if (gameService.gameActive(chatId)) {
            return texts.gameAlreadyRunning(locale);
        }
        menuMessenger.editStarting(chatId, messageId, categoryLabel(slug, locale),
                texts.difficultyLabel(difficultyToken, locale), locale);
        gameService.startQuiz(chatId, chatId < 0, userId, name, category, difficulty, locale);
        return "";
    }

    private String categoryLabel(String slug, Locale locale) {
        if ("any".equals(slug)) {
            return texts.btnAnyCategory(locale);
        }
        return categoryService.name(slug, locale);
    }

    private static TopScope parseScope(String value) {
        try {
            return TopScope.valueOf(value);
        } catch (IllegalArgumentException e) {
            return TopScope.GROUP;
        }
    }
}
