package com.quizarena.bot;

import com.quizarena.domain.Category;
import com.quizarena.domain.PersonalRank;
import com.quizarena.domain.Profile;
import com.quizarena.domain.Standing;
import com.quizarena.domain.TopScope;
import com.quizarena.handler.MessageBuilder;
import com.quizarena.handler.UiTexts;
import com.quizarena.render.BannerRenderer;
import com.quizarena.render.LeaderboardCardRenderer;
import com.quizarena.render.ProfileCardRenderer;
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

    private final TelegramClient telegramClient;
    private final UiTexts texts;
    private final MessageBuilder messageBuilder;
    private final BannerRenderer bannerRenderer;
    private final ProfileCardRenderer profileCardRenderer;
    private final LeaderboardCardRenderer leaderboardCardRenderer;

    public MenuMessenger(TelegramClient telegramClient, UiTexts texts, MessageBuilder messageBuilder,
                         BannerRenderer bannerRenderer, ProfileCardRenderer profileCardRenderer,
                         LeaderboardCardRenderer leaderboardCardRenderer) {
        this.telegramClient = telegramClient;
        this.texts = texts;
        this.messageBuilder = messageBuilder;
        this.bannerRenderer = bannerRenderer;
        this.profileCardRenderer = profileCardRenderer;
        this.leaderboardCardRenderer = leaderboardCardRenderer;
    }

    public void sendMainMenu(long chatId, Locale locale) throws TelegramApiException {
        sendBanner(chatId, WELCOME, texts.menuHome(locale), mainMenuMarkup(locale), locale);
    }

    public void hint(long chatId, String text) throws TelegramApiException {
        telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build());
    }

    public void sendCategories(long chatId, List<Category> available, boolean privateChat, Locale locale)
            throws TelegramApiException {
        sendBanner(chatId, GAME, texts.categoriesTitle(locale),
                categoriesMarkup(available, privateChat, "m:cat", locale), locale);
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

    public void editCategories(long chatId, int messageId, List<Category> available, boolean privateChat, Locale locale)
            throws TelegramApiException {
        editBanner(chatId, messageId, GAME, texts.categoriesTitle(locale),
                categoriesMarkup(available, privateChat, "m:cat", locale), locale);
    }

    public void editDifficulties(long chatId, int messageId, String slug, String categoryLabel, Locale locale)
            throws TelegramApiException {
        editBanner(chatId, messageId, GAME, texts.difficultyTitle(locale, categoryLabel),
                difficultyMarkup(slug, "m:diff", "m:play", locale), locale);
    }

    public void editStarting(long chatId, int messageId, String categoryLabel, String difficultyLabel, Locale locale)
            throws TelegramApiException {
        editBanner(chatId, messageId, GAME, texts.starting(locale, categoryLabel, difficultyLabel),
                InlineKeyboardMarkup.builder().build(), locale);
    }

    public void sendRules(long chatId, Locale locale) throws TelegramApiException {
        telegramClient.execute(SendMessage.builder()
                .chatId(chatId).text(texts.rules(locale)).replyMarkup(backToMenuMarkup(locale))
                .parseMode("HTML").build());
    }

    public void editDuelCategories(long chatId, int messageId, List<Category> available, boolean privateChat, Locale locale)
            throws TelegramApiException {
        editBanner(chatId, messageId, DUEL, texts.categoriesTitle(locale),
                categoriesMarkup(available, privateChat, "m:dcat", locale), locale);
    }

    public void editDuelDifficulties(long chatId, int messageId, String slug, String categoryLabel, Locale locale)
            throws TelegramApiException {
        editBanner(chatId, messageId, DUEL, texts.difficultyTitle(locale, categoryLabel),
                difficultyMarkup(slug, "m:ddiff", "m:duel", locale), locale);
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

    public void sendProfileCard(long chatId, Profile profile, String name, Locale locale) {
        Thread.ofVirtual().name("profile-card").start(() -> {
            try {
                telegramClient.execute(SendPhoto.builder().chatId(chatId)
                        .photo(photo(profileCardRenderer.render(profile, name, locale)))
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

    private InlineKeyboardMarkup categoriesMarkup(List<Category> available, boolean privateChat, String prefix,
                                                  Locale locale) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (int i = 0; i < available.size(); i += 2) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(button(texts.categoryLabel(available.get(i), locale), prefix + ":" + available.get(i).slug()));
            if (i + 1 < available.size()) {
                row.add(button(texts.categoryLabel(available.get(i + 1), locale), prefix + ":" + available.get(i + 1).slug()));
            }
            rows.add(row);
        }
        rows.add(new InlineKeyboardRow(button(texts.btnAnyCategory(locale), prefix + ":any")));
        if (privateChat) {
            rows.add(new InlineKeyboardRow(button(texts.btnBack(locale), "m:home")));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardMarkup difficultyMarkup(String slug, String prefix, String back, Locale locale) {
        return InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(button(texts.difficultyLabel("easy", locale), prefix + ":" + slug + ":easy"),
                        button(texts.difficultyLabel("medium", locale), prefix + ":" + slug + ":medium")),
                new InlineKeyboardRow(button(texts.difficultyLabel("hard", locale), prefix + ":" + slug + ":hard"),
                        button(texts.btnAnyDifficulty(locale), prefix + ":" + slug + ":any")),
                new InlineKeyboardRow(button(texts.btnBack(locale), back)))).build();
    }

    private InlineKeyboardMarkup leaderboardMarkup(Locale locale) {
        return InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(
                        button(texts.scopeLabel(TopScope.GROUP, locale), "m:lb:GROUP"),
                        button(texts.scopeLabel(TopScope.WEEK, locale), "m:lb:WEEK"),
                        button(texts.scopeLabel(TopScope.GLOBAL, locale), "m:lb:GLOBAL")),
                new InlineKeyboardRow(button(texts.btnBack(locale), "m:menu")))).build();
    }

    private InlineKeyboardMarkup languageMarkup(Locale locale) {
        return InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(button(texts.languageName("ru", locale), "m:setlang:ru"),
                        button(texts.languageName("en", locale), "m:setlang:en")),
                new InlineKeyboardRow(button(texts.btnBack(locale), "m:home")))).build();
    }

    private InlineKeyboardMarkup backToMenuMarkup(Locale locale) {
        return InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(button(texts.btnBack(locale), "m:menu")))).build();
    }

    private InlineKeyboardButton button(String text, String data) {
        return InlineKeyboardButton.builder().text(text).callbackData(data).build();
    }
}
