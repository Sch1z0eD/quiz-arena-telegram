package com.quizarena.service;

import com.quizarena.bot.MenuMessenger;
import com.quizarena.handler.UiTexts;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MenuServiceTest {

    private static final Locale EN = Locale.of("en");
    private static final long GROUP = -1001L;
    private static final long PRIVATE = 42L;

    private final GameService gameService = mock(GameService.class);
    private final MenuMessenger menuMessenger = mock(MenuMessenger.class);
    private final UiTexts texts = mock(UiTexts.class);
    private final DuelService duelService = mock(DuelService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final MenuService service = new MenuService(gameService, menuMessenger, texts, duelService,
            mock(LocaleService.class), mock(AvatarService.class), categoryService);

    @Test
    void groupChatCannotStartDuelSearch() throws Exception {
        when(texts.privateOnly(EN)).thenReturn("dm only");
        String toast = service.navigate(GROUP, 1, 7L, "Bob", "m:dsearch:any:any", EN);
        assertEquals("dm only", toast);
        verifyNoInteractions(duelService);
    }

    @Test
    void groupChatCannotOpenMainMenu() throws Exception {
        when(texts.privateOnly(EN)).thenReturn("dm only");
        String toast = service.navigate(GROUP, 1, 7L, "Bob", "m:home", EN);
        assertEquals("dm only", toast);
        verify(menuMessenger, never()).editMainMenu(anyLong(), anyInt(), any());
    }

    @Test
    void groupChatCanOpenRankCard() throws Exception {
        service.navigate(GROUP, 1, 7L, "Bob", "m:rank", EN);
        verify(menuMessenger).deleteMessage(GROUP, 1);
        verify(gameService).showRank(GROUP, 7L, "Bob", EN);
    }

    @Test
    void groupChatCanPickQuizCategory() throws Exception {
        when(gameService.availableDifficulties("science", "en"))
                .thenReturn(new GameService.DifficultyOptions(List.of("easy"), true));
        service.navigate(GROUP, 1, 7L, "Bob", "m:cat:science", EN);
        verify(menuMessenger).editDifficulties(eq(GROUP), eq(1), eq("science"), any(), any(), anyBoolean(), eq(EN));
    }

    @Test
    void privateChatCanOpenMainMenu() throws Exception {
        service.navigate(PRIVATE, 1, 7L, "Bob", "m:home", EN);
        verify(menuMessenger).editMainMenu(PRIVATE, 1, EN);
    }

    @Test
    void categoryPagingForwardsRequestedPage() throws Exception {
        when(gameService.availableCategories("en")).thenReturn(List.of("science"));
        service.navigate(PRIVATE, 5, 7L, "Bob", "m:catpg:2", EN);
        verify(menuMessenger).editCategories(eq(PRIVATE), eq(5), any(), eq(2), eq(true), eq(EN));
    }

    @Test
    void categoryPagingAllowedInGroup() throws Exception {
        when(gameService.availableCategories("en")).thenReturn(List.of("science"));
        String toast = service.navigate(GROUP, 5, 7L, "Bob", "m:catpg:1", EN);
        assertEquals("", toast);
        verify(menuMessenger).editCategories(eq(GROUP), eq(5), any(), eq(1), eq(false), eq(EN));
    }

    @Test
    @SuppressWarnings("unchecked")
    void categoriesAreOrderedByLocalizedName() throws Exception {
        when(gameService.availableCategories("en")).thenReturn(List.of("a", "b"));
        when(categoryService.name("a", EN)).thenReturn("Zebra");
        when(categoryService.name("b", EN)).thenReturn("Apple");

        service.navigate(PRIVATE, 5, 7L, "Bob", "m:play", EN);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(menuMessenger).editCategories(eq(PRIVATE), eq(5), captor.capture(), eq(0), eq(true), eq(EN));
        assertEquals(List.of("b", "a"), captor.getValue());
    }
}
