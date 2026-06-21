package com.quizarena.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizationMessagesTest {

    private static final Locale RU = Locale.of("ru");
    private static final Locale EN = Locale.of("en");
    private static final Pattern CYRILLIC = Pattern.compile("\\p{IsCyrillic}");

    private final Localizer localizer = new Localizer(messageSource(), new Plurals());

    @Test
    void resolvesBothLocalesFromTheirOwnBundle() {
        String ru = localizer.get(RU, "menu.home");
        String en = localizer.get(EN, "menu.home");
        assertTrue(CYRILLIC.matcher(ru).find(), "RU menu.home should be Russian");
        assertFalse(CYRILLIC.matcher(en).find(), "EN menu.home should fall back to the English base bundle");
        assertNotEquals(ru, en);
    }

    @Test
    void resolvesEveryKeyMessengersUseForBothLocales() {
        for (String key : new String[]{
                "menu.home", "menu.categoriesTitle", "menu.languageTitle", "lobby.joinButton",
                "question.title", "reveal.answer", "scoreboard.title", "rank.title",
                "duel.searching", "rules.text", "profile.text", "error.generic",
                "btn.play", "btn.language", "difficulty.easy",
                "scope.GROUP", "card.score", "card.duelFinished", "lang.ru",
                "inline.duel.title", "inline.duel.message", "inline.duel.button",
                "inline.play.title", "inline.play.message", "duel.opponentUnavailable",
                "card.matchupFound", "difficulty.any", "btn.mainMenu", "btn.rank"}) {
            assertFalse(localizer.get(RU, key).isBlank(), "missing RU: " + key);
            assertFalse(localizer.get(EN, key).isBlank(), "missing EN: " + key);
        }
    }

    @Test
    void appliesSlavicPluralForms() {
        assertTrue(localizer.plural(RU, "noun.points", 1).contains("1"));
        assertNotEquals(
                localizer.plural(RU, "noun.points", 2),
                localizer.plural(RU, "noun.points", 5),
                "RU few (2) and many (5) must differ");
        assertTrue(localizer.plural(EN, "noun.points", 5).contains("5"));
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
