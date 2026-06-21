package com.quizarena.integration;

import com.quizarena.domain.AnswerRecord;
import com.quizarena.domain.DuelRecord;
import com.quizarena.domain.Question;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.DuelRepository;
import com.quizarena.repository.QuestionRepository;
import com.quizarena.repository.UserRepository;
import com.quizarena.repository.UserRepository.UserRowProjection;
import com.quizarena.service.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserQueryIT extends AbstractIntegrationTest {

    private static final long PLAYER = 7400001L;
    private static final long LURKER = 7400002L;

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository users;
    @Autowired
    private AnswerRepository answers;
    @Autowired
    private DuelRepository duels;
    @Autowired
    private QuestionRepository questions;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    void seed() {
        userService.touch(human(PLAYER, "ZZQ-Player", "zzqp"));
        userService.touch(human(LURKER, "ZZQ-Lurker", "zzql"));
        jdbc.update("INSERT INTO user_rating(user_id, elo) VALUES (?, 1300) ON CONFLICT (user_id) DO UPDATE SET elo = 1300", PLAYER);
        jdbc.update("INSERT INTO user_settings(user_id, language) VALUES (?, 'ru') ON CONFLICT (user_id) DO UPDATE SET language = 'ru'", PLAYER);

        long qid = questions.save(new Question("zzq?", "A", "B", "C", "D", 0, "zzq-cat", "easy", "zzq", "h-zzq-1")).getId();
        // Two GAME games (8801 correct, 8802 wrong) plus a DUEL answer reusing game_id 8801 to prove the
        // GAME filter excludes duels even when the id namespaces collide.
        answers.save(answer(8801L, PLAYER, qid, true, "GAME", 1000L));
        answers.save(answer(8802L, PLAYER, qid, false, "GAME", 2000L));
        answers.save(answer(8801L, PLAYER, qid, true, "DUEL", 3000L));
    }

    @Test
    void listAggregatesPlayerAndKeepsNonPlayingContact() {
        Page<UserRowProjection> page = users.searchUsers("ZZQ", 0L, PageRequest.of(0, 10, Sort.by("id")));
        List<UserRowProjection> rows = page.getContent();
        assertEquals(2, rows.size(), "both the player and the contacted-but-never-played user appear");
        assertEquals(PLAYER, rows.get(0).getId());
        assertEquals(LURKER, rows.get(1).getId());

        UserRowProjection player = rows.get(0);
        assertEquals(2, player.getGames(), "games counts only GAME-mode games, excluding the duel that reused game_id 8801");
        assertNotNull(player.getAccuracy());
        assertTrue(player.getAccuracy() > 66 && player.getAccuracy() < 67, "accuracy covers all answers incl. the duel: 2/3");
        assertEquals(1300, player.getElo());
        assertEquals("ru", player.getLanguage());

        UserRowProjection lurker = rows.get(1);
        assertEquals(0, lurker.getGames());
        assertNull(lurker.getAccuracy(), "no answers must not divide by zero");
        assertEquals(1000, lurker.getElo(), "missing rating falls back to 1000");
    }

    @Test
    void searchMatchesIdOrName() {
        assertTrue(contains(users.searchUsers("7400001", PLAYER, PageRequest.of(0, 10)), PLAYER), "exact id match");
        Page<UserRowProjection> byName = users.searchUsers("ZZQ-Lurker", 0L, PageRequest.of(0, 10));
        assertEquals(1, byName.getTotalElements());
        assertEquals(LURKER, byName.getContent().get(0).getId());
    }

    @Test
    void paginates() {
        Page<UserRowProjection> firstPage = users.searchUsers("ZZQ", 0L, PageRequest.of(0, 1, Sort.by("id")));
        assertEquals(2, firstPage.getTotalElements());
        assertEquals(1, firstPage.getContent().size());
        assertEquals(2, firstPage.getTotalPages());
    }

    @Test
    void detailHeaderIsUserScopedAndBreakdownsDerive() {
        UserRowProjection header = users.findUserHeader(PLAYER).orElseThrow();
        assertEquals(2, header.getGames());
        assertTrue(header.getAccuracy() > 66 && header.getAccuracy() < 67);

        AnswerRepository.UserCategoryRow category = answers.userCategoryBreakdown(PLAYER).stream()
                .filter(row -> "zzq-cat".equals(row.getCategory())).findFirst().orElseThrow();
        assertEquals(3, category.getAnswered());
        assertEquals(2, category.getCorrect());

        List<AnswerRepository.UserGameRow> recent = answers.recentGames(PLAYER, 10);
        assertEquals(3, recent.size(), "two GAME rounds + one DUEL round");
        // game_id 8801 exists as both a GAME and a DUEL round: grouping by (game_id, mode) must keep them apart.
        long collisionRows = recent.stream().filter(row -> row.getGameId() == 8801L).count();
        assertEquals(2, collisionRows, "a solo game and a duel sharing game_id 8801 must stay two rows");
        assertTrue(recent.stream().anyMatch(row -> row.getGameId() == 8801L && "GAME".equals(row.getMode())));
        assertTrue(recent.stream().anyMatch(row -> row.getGameId() == 8801L && "DUEL".equals(row.getMode())));

        duels.save(new DuelRecord(74001L, PLAYER, 999L, 3, 1, PLAYER, "zzq-cat", "easy", 4000L));
        duels.save(new DuelRecord(74002L, 999L, PLAYER, 1, 3, 999L, "zzq-cat", "easy", 5000L));
        duels.save(new DuelRecord(74003L, PLAYER, 999L, 2, 2, null, "zzq-cat", "easy", 6000L));
        DuelRepository.DuelRecordRow record = duels.duelRecord(PLAYER);
        assertEquals(3, record.getPlayed());
        assertEquals(1, record.getWins());
        assertEquals(1, record.getDraws());
        assertEquals(1, record.getPlayed() - record.getWins() - record.getDraws(), "losses derive from the rest");
    }

    @Test
    void unknownIdHasNoHeader() {
        Optional<UserRowProjection> header = users.findUserHeader(7409999L);
        assertTrue(header.isEmpty(), "an id absent from users yields no header (404 in the service)");
    }

    private static boolean contains(Page<UserRowProjection> page, long id) {
        return page.getContent().stream().anyMatch(row -> row.getId() == id);
    }

    private static AnswerRecord answer(long gameId, long userId, long questionId, boolean correct, String mode, long ts) {
        return new AnswerRecord(gameId, 1L, userId, questionId, correct, 0, ts, mode);
    }

    private static User human(long id, String firstName, String username) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getIsBot()).thenReturn(false);
        when(user.getFirstName()).thenReturn(firstName);
        when(user.getUserName()).thenReturn(username);
        return user;
    }
}
