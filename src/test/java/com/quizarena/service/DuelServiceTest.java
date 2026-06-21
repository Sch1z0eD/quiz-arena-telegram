package com.quizarena.service;

import com.quizarena.bot.BotIdentity;
import com.quizarena.bot.DuelMessenger;
import com.quizarena.config.DuelProperties;
import com.quizarena.domain.DuelInvite;
import com.quizarena.domain.OptionOrder;
import com.quizarena.domain.Question;
import com.quizarena.domain.RecordResult;
import com.quizarena.i18n.Localizer;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.DuelRepository;
import com.quizarena.repository.DuelStore;
import com.quizarena.repository.GameStore;
import com.quizarena.repository.InviteStore;
import com.quizarena.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DuelServiceTest {

    private final DuelStore store = mock(DuelStore.class);
    private final DuelMessenger messenger = mock(DuelMessenger.class);
    private final GameStore gameStore = mock(GameStore.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final AnswerRepository answers = mock(AnswerRepository.class);
    private final DuelRepository duels = mock(DuelRepository.class);
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final DuelProperties properties = new DuelProperties(45, 20, 1, 100);
    private final LocaleService localeService = mock(LocaleService.class);
    private final EloService eloService = mock(EloService.class);
    private final AvatarService avatarService = mock(AvatarService.class);
    private final InviteStore inviteStore = mock(InviteStore.class);
    private final BotIdentity botIdentity = mock(BotIdentity.class);
    private final Localizer localizer = mock(Localizer.class);
    private final OptionShuffler shuffler = mock(OptionShuffler.class);

    private final DuelService service = new DuelService(store, messenger, gameStore, questions, answers, duels,
            scheduler, properties, localeService, eloService, avatarService, inviteStore, botIdentity, localizer, shuffler);

    @Test
    void createInlineInviteUsesCallerAsBothUserAndChatWithAnyFilters() throws Exception {
        when(botIdentity.username()).thenReturn("Bot");
        when(inviteStore.create(any())).thenReturn("TOK");

        DuelService.Invitation invitation = service.createInlineInvite(5L, "Zoe", Locale.of("ru"));

        ArgumentCaptor<DuelInvite> captor = ArgumentCaptor.forClass(DuelInvite.class);
        verify(inviteStore).create(captor.capture());
        DuelInvite invite = captor.getValue();
        assertEquals(5L, invite.inviterUserId());
        assertEquals(5L, invite.inviterChatId());
        assertEquals("ru", invite.locale());
        assertEquals("", invite.category(), "inline has no picker, so category is any");
        assertEquals("", invite.difficulty(), "inline has no picker, so difficulty is any");
        assertEquals("Zoe", invite.inviterName());
        assertEquals("TOK", invitation.token());
        assertEquals("https://t.me/Bot?start=duel_TOK", invitation.link());
    }

    @Test
    void scoresCorrectByMappingDisplaySlotThroughOrder() {
        long duelId = 7L, userA = 10L, userB = 20L;
        // correct storage index 2; order shows storage 2 at display slot 0
        DuelStore.Snapshot snapshot = new DuelStore.Snapshot(
                0, 3, 2, 0L, List.of(100L), 10L, 20L, userA, userB,
                0, 0, "", "", "A", "B", "ru", "ru", OptionOrder.parse("2,0,3,1"));
        when(store.snapshot(duelId)).thenReturn(snapshot);
        when(store.recordAnswer(eq(duelId), anyInt(), anyLong(), anyLong())).thenReturn(1L);
        when(store.answeredCount(eq(duelId), anyInt())).thenReturn(1L);

        RecordResult correct = service.recordAnswer(duelId, userA, 1L, 0); // slot 0 -> storage 2 = correct
        RecordResult wrong = service.recordAnswer(duelId, userB, 1L, 1);   // slot 1 -> storage 0 != correct

        assertTrue(correct.correct(), "answering the displayed slot of the correct option must score correct");
        assertFalse(wrong.correct(), "answering another slot must not score correct");
    }

    @Test
    void invitedDuelRollsBackAndFreesBothWhenInviterUnreachable() throws Exception {
        long inviter = 10L, friend = 20L, duelId = 1L;
        when(inviteStore.claim("tk")).thenReturn(Optional.of(new DuelInvite(inviter, inviter, "ru", "", "", "Inv")));
        when(gameStore.gameActive(anyLong())).thenReturn(false);
        when(store.isBusy(anyLong())).thenReturn(false);
        when(localeService.parse("ru")).thenReturn(Locale.of("ru"));
        when(localeService.parse("en")).thenReturn(Locale.of("en"));
        Question question = mock(Question.class);
        when(question.getId()).thenReturn(100L);
        when(question.getCorrectOption()).thenReturn(0);
        when(questions.findRandomFiltered("", "", "ru", 1)).thenReturn(List.of(question));
        when(questions.findById(100L)).thenReturn(Optional.of(question));
        when(store.nextId()).thenReturn(duelId);
        when(store.nextToken()).thenReturn(1L);
        when(store.snapshot(duelId)).thenReturn(new DuelStore.Snapshot(
                -1, 1, -1, 0L, List.of(100L), inviter, friend, inviter, friend,
                0, 0, "", "", "Inv", "Fr", "ru", "en", OptionOrder.identity()));
        lenient().when(localizer.get(any(Locale.class), anyString())).thenAnswer(i -> i.getArgument(1));
        when(shuffler.next()).thenReturn(OptionOrder.identity());
        // The very first question is sent to the inviter (chatA). If they never started the bot, this 403s.
        when(messenger.sendQuestion(eq(inviter), any(), any(), anyInt(), anyInt(), anyLong(), anyLong(), any()))
                .thenThrow(new TelegramApiException("403 forbidden"));

        boolean started = service.acceptInvite("tk", friend, friend, "Fr", Locale.of("en"));

        assertTrue(started, "claim already consumed the token; launch handles its own failure internally");
        verify(store).cleanup(duelId, 1, inviter, friend);
        verify(messenger).notify(friend, "duel.opponentUnavailable");
    }
}
