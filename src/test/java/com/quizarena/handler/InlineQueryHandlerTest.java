package com.quizarena.handler;

import com.quizarena.bot.BotIdentity;
import com.quizarena.bot.InlineMessenger;
import com.quizarena.config.BrandProperties;
import com.quizarena.i18n.Localizer;
import com.quizarena.i18n.Plurals;
import com.quizarena.service.DuelService;
import com.quizarena.service.LocaleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InlineQueryHandlerTest {

    private static final Pattern CYRILLIC = Pattern.compile("\\p{IsCyrillic}");

    private final DuelService duelService = mock(DuelService.class);
    private final LocaleService localeService = mock(LocaleService.class);
    private final InlineMessenger messenger = mock(InlineMessenger.class);
    private final BotIdentity botIdentity = mock(BotIdentity.class);
    private final UiTexts texts =
            new UiTexts(new Localizer(messageSource(), new Plurals()), new BrandProperties("QuizArena", "@QuizArenaBot"));
    private final InlineQueryHandler handler =
            new InlineQueryHandler(duelService, localeService, texts, messenger, botIdentity);

    @Test
    void postsLocalizedDuelAndPlayResultsForRussianUser() throws Exception {
        List<InlineQueryResult> results = handle("ru", 7L, "Алиса", "tok123");

        assertEquals(2, results.size());
        InlineQueryResultArticle duel = (InlineQueryResultArticle) results.get(0);
        InlineQueryResultArticle play = (InlineQueryResultArticle) results.get(1);

        assertEquals("duel", duel.getId());
        assertEquals("play", play.getId());
        assertTrue(CYRILLIC.matcher(duel.getTitle()).find(), "RU duel title should be Russian");
        assertTrue(messageText(duel).contains("Алиса"), "posted message embeds the inviter name");
        assertTrue(CYRILLIC.matcher(messageText(duel)).find());
        assertEquals("https://t.me/QuizArenaBot2?start=duel_tok123", buttonUrl(duel));
        assertEquals("https://t.me/QuizArenaBot2?start=play", buttonUrl(play));
    }

    @Test
    void postsEnglishResultsForEnglishUser() throws Exception {
        List<InlineQueryResult> results = handle("en", 8L, "Bob", "tokEN");

        InlineQueryResultArticle duel = (InlineQueryResultArticle) results.get(0);
        assertFalse(CYRILLIC.matcher(duel.getTitle()).find(), "EN duel title should be English");
        assertEquals("https://t.me/QuizArenaBot2?start=duel_tokEN", buttonUrl(duel));
    }

    @SuppressWarnings("unchecked")
    private List<InlineQueryResult> handle(String lang, long userId, String name, String token) throws Exception {
        Locale locale = Locale.of(lang);
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getFirstName()).thenReturn(name);
        InlineQuery query = mock(InlineQuery.class);
        when(query.getId()).thenReturn("q-" + lang);
        when(query.getFrom()).thenReturn(user);
        when(localeService.resolve(eq(userId), any())).thenReturn(locale);
        when(botIdentity.username()).thenReturn("QuizArenaBot2");
        when(duelService.createInlineInvite(userId, name, locale))
                .thenReturn(new DuelService.Invitation(token, "https://t.me/QuizArenaBot2?start=duel_" + token));

        handler.handle(query);

        ArgumentCaptor<List<InlineQueryResult>> captor = ArgumentCaptor.forClass(List.class);
        verify(messenger).answer(eq("q-" + lang), captor.capture());
        return captor.getValue();
    }

    private static String messageText(InlineQueryResultArticle article) {
        return ((InputTextMessageContent) article.getInputMessageContent()).getMessageText();
    }

    private static String buttonUrl(InlineQueryResultArticle article) {
        return article.getReplyMarkup().getKeyboard().get(0).get(0).getUrl();
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
