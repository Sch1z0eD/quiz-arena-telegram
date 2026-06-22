package com.quizarena.bot;

import com.quizarena.domain.PersonalRank;
import com.quizarena.domain.Profile;
import com.quizarena.domain.Standing;
import com.quizarena.domain.TopScope;
import com.quizarena.handler.MessageBuilder;
import com.quizarena.handler.UiTexts;
import com.quizarena.render.BannerRenderer;
import com.quizarena.render.LeaderboardCardRenderer;
import com.quizarena.domain.Language;
import com.quizarena.render.ProfileCardRenderer;
import com.quizarena.service.CategoryService;
import com.quizarena.service.LanguageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class MenuMessenger {

    private static final Logger log = LoggerFactory.getLogger(MenuMessenger.class);

    private static final String WELCOME = "welcome_banner.svg";
    private static final String GAME = "game_banner.svg";
    private static final String DUEL = "duel_banner.svg";
    private static final String RULES = "rules_banner.svg";
    private static final int CAPTION_LIMIT = 1024;
    private static final int CATEGORY_PAGE_SIZE = 8;

    private final TelegramClient telegramClient;
    private final UiTexts texts;
    private final MessageBuilder messageBuilder;
    private final BannerRenderer bannerRenderer;
    private final ProfileCardRenderer profileCardRenderer;
    private final LeaderboardCardRenderer leaderboardCardRenderer;
    private final CategoryService categoryService;
    private final LanguageRegistry languageRegistry;

    public MenuMessenger(TelegramClient telegramClient, UiTexts texts, MessageBuilder messageBuilder,
                         BannerRenderer bannerRenderer, ProfileCardRenderer profileCardRenderer,
                         LeaderboardCardRenderer leaderboardCardRenderer, CategoryService categoryService,
                         LanguageRegistry languageRegistry) {
        this.telegramClient = telegramClient;
        this.texts = texts;
        this.messageBuilder = messageBuilder;
        this.bannerRenderer = bannerRenderer;
        this.profileCardRenderer = profileCardRenderer;
        this.leaderboardCardRenderer = leaderboardCardRenderer;
        this.categoryService = categoryService;
        this.languageRegistry = languageRegistry;
    }

    public void sendMainMenu(long chatId, Locale locale) throws TelegramApiException {
        sendBanner(chatId, WELCOME, texts.menuHome(locale), mainMenuMarkup(locale), locale);
    }

    public void hint(long chatId, String text) throws TelegramApiException {
        telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build());
    }

    public void sendCategories(long chatId, List<String> ordered, int page, boolean privateChat, Locale locale)
            throws TelegramApiException {
        sendBanner(chatId, GAME, texts.categoriesTitle(locale),
                categoriesMarkup(ordered, page, privateChat, "m:cat", "m:catpg", locale), locale);
    }

    public void deleteMessage(long chatId, int messageId) {
        try {
            telegramClient.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
        } catch (Exception e) {
            log.warn("Delete message {} failed in chat {}", messageId, chatId, e);
        }
    }

    public void editMainMenu(long chatId, int messageId, Locale locale) throws TelegramApiException {
        editBanner(chatId, messageId, WELCOME, texts.menuHome(locale), mainMenuMarkup(locale), locale);
    }

    public void editCategories(long chatId, int messageId, List<String> ordered, int page, boolean privateChat, Locale locale)
            throws TelegramApiException {
        editBanner(chatId, messageId, GAME, texts.categoriesTitle(locale),
                categoriesMarkup(ordered, page, privateChat, "m:cat", "m:catpg", locale), locale);
    }

    public void editDifficulties(long chatId, int messageId, String slug, String categoryLabel,
                                 List<String> difficulties, boolean anyAvailable, Locale locale)
            throws TelegramApiException {
        editBanner(chatId, messageId, GAME, texts.difficultyTitle(locale, categoryLabel),
                difficultyMarkup(slug, "m:diff", "m:play", difficulties, anyAvailable, locale), locale);
    }

    public void editStarting(long chatId, int messageId, String categoryLabel, String difficultyLabel, Locale locale)
            throws TelegramApiException {
        editBanner(chatId, messageId, GAME, texts.starting(locale, categoryLabel, difficultyLabel),
                InlineKeyboardMarkup.builder().build(), locale);
    }

    public void sendRules(long chatId, Locale locale) throws TelegramApiException {
        String rules = texts.rules(locale);
        if (rules.length() <= CAPTION_LIMIT) {
            telegramClient.execute(SendPhoto.builder().chatId(chatId)
                    .photo(photo(bannerRenderer.render(RULES, locale)))
                    .caption(rules).parseMode("HTML").replyMarkup(backToMenuMarkup(locale)).build());
            return;
        }
        log.warn("Rules text {} chars exceeds caption limit, sending as plain text in chat {}", rules.length(), chatId);
        telegramClient.execute(SendMessage.builder()
                .chatId(chatId).text(rules).replyMarkup(backToMenuMarkup(locale)).parseMode("HTML").build());
    }

    public void editDuelCategories(long chatId, int messageId, List<String> ordered, int page, boolean privateChat, Locale locale)
            throws TelegramApiException {
        editBanner(chatId, messageId, DUEL, texts.categoriesTitle(locale),
                categoriesMarkup(ordered, page, privateChat, "m:dcat", "m:dcatpg", locale), locale);
    }

    public void editDuelDifficulties(long chatId, int messageId, String slug, String categoryLabel,
                                     List<String> difficulties, boolean anyAvailable, Locale locale)
            throws TelegramApiException {
        editBanner(chatId, messageId, DUEL, texts.difficultyTitle(locale, categoryLabel),
                difficultyMarkup(slug, "m:ddiff", "m:duel", difficulties, anyAvailable, locale), locale);
    }

    public void editDuelMode(long chatId, int messageId, String slug, String difficulty, Locale locale)
            throws TelegramApiException {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(button(texts.btnDuelRandom(locale), "m:dsearch:" + slug + ":" + difficulty)),
                new InlineKeyboardRow(button(texts.btnDuelInvite(locale), "m:dinvite:" + slug + ":" + difficulty)),
                new InlineKeyboardRow(button(texts.btnBack(locale), "m:dcat:" + slug)))).build();
        editBanner(chatId, messageId, DUEL, texts.duelModeTitle(locale), markup, locale);
    }

    public void editDuelInvite(long chatId, int messageId, String link, String token, Locale locale)
            throws TelegramApiException {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(button(texts.btnCancelInvite(locale), "m:dcancelinvite:" + token)))).build();
        editBanner(chatId, messageId, DUEL, texts.inviteShare(locale, link), markup, locale);
    }

    public void editLanguageMenu(long chatId, int messageId, Locale locale) throws TelegramApiException {
        editBanner(chatId, messageId, WELCOME, texts.languageTitle(locale), languageMarkup(locale), locale);
    }

    public void sendProfileCard(long chatId, Profile profile, String name, byte[] avatar, Locale locale) {
        Thread.ofVirtual().name("profile-card").start(() -> {
            try {
                telegramClient.execute(SendPhoto.builder().chatId(chatId)
                        .photo(photo(profileCardRenderer.render(profile, name, avatar, locale)))
                        .caption(messageBuilder.eloCaption(locale, profile.elo())).parseMode("HTML")
                        .replyMarkup(backToMenuMarkup(locale)).build());
            } catch (Exception e) {
                log.warn("Profile card failed in chat {}, falling back to text", chatId, e);
                trySendText(chatId, texts.profile(locale, profile), backToMenuMarkup(locale));
            }
        });
    }

    public void sendLeaderboardCard(long chatId, TopScope scope, List<Standing> top, PersonalRank personal, Locale locale) {
        Thread.ofVirtual().name("leaderboard-card").start(() -> {
            try {
                telegramClient.execute(SendPhoto.builder().chatId(chatId)
                        .photo(photo(leaderboardCardRenderer.render(scope, top, locale)))
                        .replyMarkup(leaderboardMarkup(locale)).build());
            } catch (Exception e) {
                log.warn("Leaderboard card failed in chat {}, falling back to text", chatId, e);
                trySendText(chatId, messageBuilder.topText(locale, scope, top, personal), backToMenuMarkup(locale));
            }
        });
    }

    public void editLeaderboardCard(long chatId, int messageId, TopScope scope,
                                    List<Standing> top, PersonalRank personal, Locale locale) {
        Thread.ofVirtual().name("leaderboard-card").start(() -> {
            try {
                InputMediaPhoto media = InputMediaPhoto.builder()
                        .media(new ByteArrayInputStream(leaderboardCardRenderer.render(scope, top, locale)), "card.png")
                        .build();
                telegramClient.execute(EditMessageMedia.builder()
                        .chatId(chatId).messageId(messageId).media(media)
                        .replyMarkup(leaderboardMarkup(locale)).build());
            } catch (Exception e) {
                if (isNotModified(e)) {
                    return;
                }
                log.warn("Leaderboard switch failed in chat {}, falling back to text", chatId, e);
                trySendText(chatId, messageBuilder.topText(locale, scope, top, personal), backToMenuMarkup(locale));
            }
        });
    }

    private void sendBanner(long chatId, String banner, String caption, InlineKeyboardMarkup markup, Locale locale)
            throws TelegramApiException {
        telegramClient.execute(SendPhoto.builder()
                .chatId(chatId).photo(photo(bannerRenderer.render(banner, locale)))
                .caption(caption).parseMode("HTML").replyMarkup(markup).build());
    }

    private void editBanner(long chatId, int messageId, String banner, String caption,
                            InlineKeyboardMarkup markup, Locale locale) throws TelegramApiException {
        InputMediaPhoto media = InputMediaPhoto.builder()
                .media(new ByteArrayInputStream(bannerRenderer.render(banner, locale)), "banner.png")
                .caption(caption).parseMode("HTML").build();
        try {
            telegramClient.execute(EditMessageMedia.builder()
                    .chatId(chatId).messageId(messageId).media(media).replyMarkup(markup).build());
        } catch (TelegramApiException e) {
            if (!isNotModified(e)) {
                throw e;
            }
        }
    }

    private InputFile photo(byte[] png) {
        return new InputFile(new ByteArrayInputStream(png), "card.png");
    }

    private void trySendText(long chatId, String text, InlineKeyboardMarkup markup) {
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId).text(text).replyMarkup(markup).parseMode("HTML").build());
        } catch (Exception e) {
            log.error("Text fallback failed in chat {}", chatId, e);
        }
    }

    private static boolean isNotModified(Exception e) {
        return e.getMessage() != null && e.getMessage().contains("message is not modified");
    }

    private InlineKeyboardMarkup mainMenuMarkup(Locale locale) {
        return InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(button(texts.btnPlay(locale), "m:play"), button(texts.btnDuel(locale), "m:duel")),
                new InlineKeyboardRow(button(texts.btnProfile(locale), "m:profile"),
                        button(texts.btnLeaderboard(locale), "m:board")),
                new InlineKeyboardRow(button(texts.btnRules(locale), "m:rules"),
                        button(texts.btnLanguage(locale), "m:lang")))).build();
    }

    private InlineKeyboardMarkup categoriesMarkup(List<String> ordered, int page, boolean privateChat,
                                                  String prefix, String pageToken, Locale locale) {
        Paged<String> paged = Paged.of(ordered, page, CATEGORY_PAGE_SIZE);
        List<String> slugs = paged.items();
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (int i = 0; i < slugs.size(); i += 2) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(button(categoryService.name(slugs.get(i), locale), prefix + ":" + slugs.get(i)));
            if (i + 1 < slugs.size()) {
                row.add(button(categoryService.name(slugs.get(i + 1), locale), prefix + ":" + slugs.get(i + 1)));
            }
            rows.add(row);
        }
        if (paged.hasPrev() || paged.hasNext()) {
            InlineKeyboardRow nav = new InlineKeyboardRow();
            if (paged.hasPrev()) {
                nav.add(button("◀", pageToken + ":" + (paged.page() - 1)));
            }
            if (paged.hasNext()) {
                nav.add(button("▶", pageToken + ":" + (paged.page() + 1)));
            }
            rows.add(nav);
        }
        rows.add(new InlineKeyboardRow(button(texts.btnAnyCategory(locale), prefix + ":any")));
        if (privateChat) {
            rows.add(new InlineKeyboardRow(button(texts.btnBack(locale), "m:home")));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardMarkup difficultyMarkup(String slug, String prefix, String back,
                                                  List<String> difficulties, boolean anyAvailable, Locale locale) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (int i = 0; i < difficulties.size(); i += 2) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(button(texts.difficultyLabel(difficulties.get(i), locale), prefix + ":" + slug + ":" + difficulties.get(i)));
            if (i + 1 < difficulties.size()) {
                row.add(button(texts.difficultyLabel(difficulties.get(i + 1), locale),
                        prefix + ":" + slug + ":" + difficulties.get(i + 1)));
            }
            rows.add(row);
        }
        if (anyAvailable) {
            rows.add(new InlineKeyboardRow(button(texts.btnAnyDifficulty(locale), prefix + ":" + slug + ":any")));
        }
        rows.add(new InlineKeyboardRow(button(texts.btnBack(locale), back)));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardMarkup leaderboardMarkup(Locale locale) {
        return InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(
                        button(texts.scopeLabel(TopScope.GROUP, locale), "m:lb:GROUP"),
                        button(texts.scopeLabel(TopScope.WEEK, locale), "m:lb:WEEK"),
                        button(texts.scopeLabel(TopScope.GLOBAL, locale), "m:lb:GLOBAL")),
                new InlineKeyboardRow(button(texts.btnRank(locale), "m:rank"),
                        button(texts.btnBack(locale), "m:menu")))).build();
    }

    private InlineKeyboardMarkup languageMarkup(Locale locale) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (Language language : languageRegistry.enabled()) {
            row.add(button(texts.languageName(language.code(), locale, language.name()), "m:setlang:" + language.code()));
            if (row.size() == 2) {
                rows.add(row);
                row = new InlineKeyboardRow();
            }
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        rows.add(new InlineKeyboardRow(button(texts.btnBack(locale), "m:home")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardMarkup backToMenuMarkup(Locale locale) {
        return InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(button(texts.btnBack(locale), "m:menu")))).build();
    }

    private InlineKeyboardButton button(String text, String data) {
        return InlineKeyboardButton.builder().text(text).callbackData(data).build();
    }
}
