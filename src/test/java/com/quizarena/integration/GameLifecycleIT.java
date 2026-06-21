package com.quizarena.integration;

import com.quizarena.config.GameProperties;
import com.quizarena.domain.PersonalRank;
import com.quizarena.domain.RecordResult;
import com.quizarena.domain.TopScope;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.GameStore;
import com.quizarena.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameLifecycleIT extends AbstractIntegrationTest {

    private static final long CHAT = 555L;
    private static final long USER = 4242L;

    @Autowired
    private GameService gameService;
    @Autowired
    private GameStore store;
    @Autowired
    private AnswerRepository answers;
    @Autowired
    private GameProperties properties;
    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void stubQuestionSends() throws Exception {
        // sendQuestion reads the returned Message id; the rest of the sends ignore their result.
        Message sent = mock(Message.class);
        when(sent.getMessageId()).thenReturn(1);
        when(telegramClient.execute(any(SendMessage.class))).thenReturn(sent);
        // GameService keeps the returned future in a ConcurrentHashMap (which rejects null), so the mocked
        // scheduler must return an inert future. The timer task never fires; the test drives each finish.
        // doReturn avoids the ScheduledFuture<?> wildcard-capture mismatch that thenReturn hits here.
        doReturn(mock(ScheduledFuture.class)).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void soloGamePersistsAnswersAndPromotesLeaderboardOnRealInfra() throws Exception {
        int total = properties.questionsPerGame();
        gameService.startQuiz(CHAT, false, USER, "Tester", "", "", Locale.of("ru"));

        for (int i = 0; i < total; i++) {
            GameStore.Snapshot snapshot = store.snapshot(CHAT);
            assertNotNull(snapshot, "game should still be running at question " + i);
            long token = currentRoundToken();
            int correctSlot = snapshot.order().displayOfCorrect(snapshot.correctOption());
            RecordResult result = gameService.recordAnswer(CHAT, USER, token, correctSlot);
            assertEquals(RecordResult.Status.ANSWERED, result.status());
            assertTrue(result.correct(), "answering the displayed slot of the correct option must score correct");
            assertTrue(result.allAnswered(), "a single player completes the round with one answer");
            gameService.finishQuestion(CHAT, token); // advances, or ends the game after the last question
        }

        assertNull(store.snapshot(CHAT), "game state is cleaned up after the final question");
        assertEquals(total, answers.countByUserId(USER), "every answer was persisted to Postgres");
        assertEquals(total, answers.countByUserIdAndCorrectTrue(USER), "all answers were correct");

        PersonalRank global = store.personal(TopScope.GLOBAL, CHAT, USER);
        assertNotNull(global, "winner was promoted to the global leaderboard");
        assertTrue(global.score() > 0, "ZINCRBY applied a positive score");
        assertEquals(1L, global.place(), "only player after a fresh flush -> first place");

        GameService.TopData top = gameService.topData(TopScope.GLOBAL, CHAT, USER);
        assertEquals(1, top.top().size());
        assertEquals(global.score(), top.top().get(0).score(), "/top and /rank agree on the score");
    }

    // White-box read of the round token (the real callback handler gets it from the answer button).
    private long currentRoundToken() {
        return Long.parseLong(redis.opsForValue().get("game:" + CHAT + ":round"));
    }
}
