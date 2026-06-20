package com.quizarena.handler;

import com.quizarena.bot.BotIdentity;
import com.quizarena.bot.InlineMessenger;
import com.quizarena.service.DuelService;
import com.quizarena.service.LocaleService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Locale;

@Component
public class InlineQueryHandler {

    private final DuelService duelService;
    private final LocaleService localeService;
    private final UiTexts texts;
    private final InlineMessenger messenger;
    private final BotIdentity botIdentity;

    public InlineQueryHandler(DuelService duelService, LocaleService localeService, UiTexts texts,
                              InlineMessenger messenger, BotIdentity botIdentity) {
        this.duelService = duelService;
        this.localeService = localeService;
        this.texts = texts;
        this.messenger = messenger;
        this.botIdentity = botIdentity;
    }

    public void handle(InlineQuery query) throws TelegramApiException {
        User from = query.getFrom();
        Locale locale = localeService.resolve(from.getId(), from.getLanguageCode());
        String name = TelegramNames.displayName(from);

        DuelService.Invitation duel = duelService.createInlineInvite(from.getId(), name, locale);
        String playLink = "https://t.me/" + botIdentity.username() + "?start=play";

        InlineQueryResult duelResult = article("duel",
                texts.inlineDuelTitle(locale), texts.inlineDuelDescription(locale),
                texts.inlineDuelMessage(locale, name), texts.inlineDuelButton(locale), duel.link());
        InlineQueryResult playResult = article("play",
                texts.inlinePlayTitle(locale), texts.inlinePlayDescription(locale),
                texts.inlinePlayMessage(locale, name), texts.inlinePlayButton(locale), playLink);

        messenger.answer(query.getId(), List.of(duelResult, playResult));
    }

    // Posted as plain text (no parse mode), so the inviter name needs no HTML escaping.
    private static InlineQueryResult article(String id, String title, String description,
                                             String messageText, String buttonText, String url) {
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder().keyboard(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text(buttonText).url(url).build()))).build();
        return InlineQueryResultArticle.builder()
                .id(id)
                .title(title)
                .description(description)
                .inputMessageContent(InputTextMessageContent.builder().messageText(messageText).build())
                .replyMarkup(keyboard)
                .build();
    }
}
