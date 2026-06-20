package com.quizarena.handler;

import com.quizarena.service.DuelService;
import com.quizarena.service.GameService;
import com.quizarena.service.LocaleService;
import com.quizarena.service.MenuService;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuizCommandHandlerTest {

    private final GameService gameService = mock(GameService.class);
    private final MenuService menuService = mock(MenuService.class);
    private final LocaleService localeService = mock(LocaleService.class);
    private final DuelService duelService = mock(DuelService.class);
    private final QuizCommandHandler handler =
            new QuizCommandHandler(gameService, menuService, localeService, duelService);

    @Test
    void startPlayOpensThePickerInUserLocale() throws Exception {
        Locale en = Locale.of("en");
        when(localeService.resolve(7L, "en")).thenReturn(en);

        handler.handle(message("/start play", 42L, 7L, "en"));

        verify(menuService).openPicker(42L, en);
        verify(menuService, never()).openMenu(anyLong(), anyBoolean(), any());
    }

    private static Message message(String text, long chatId, long userId, String lang) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getLanguageCode()).thenReturn(lang);
        Message message = mock(Message.class);
        when(message.getText()).thenReturn(text);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getFrom()).thenReturn(user);
        return message;
    }
}
