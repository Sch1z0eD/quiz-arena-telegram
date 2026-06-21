package com.quizarena.admin.web;

import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.DuelRepository;
import com.quizarena.repository.UserRepository;
import com.quizarena.repository.UserRepository.UserRowProjection;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final AnswerRepository answers = mock(AnswerRepository.class);
    private final DuelRepository duels = mock(DuelRepository.class);
    private final AdminUserService service = new AdminUserService(users, answers, duels);

    @Test
    void listRoundsAccuracyNotTruncates() {
        UserRowProjection row = projection(5L, 66.666);
        when(users.searchUsers(any(), anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));

        UserRow mapped = service.list("", PageRequest.of(0, 20)).content().get(0);

        assertEquals(67, mapped.accuracyPercent(), "accuracy must round, not truncate");
        assertEquals(5L, mapped.id());
    }

    @Test
    void listAccuracyNullWhenNoAnswers() {
        UserRowProjection row = projection(5L, null);
        when(users.searchUsers(any(), anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));

        assertNull(service.list("", PageRequest.of(0, 20)).content().get(0).accuracyPercent());
    }

    @Test
    void listParsesNumericSearchIntoIdSentinel() {
        when(users.searchUsers(any(), anyLong(), any())).thenReturn(new PageImpl<>(List.of()));

        service.list("123", PageRequest.of(0, 20));
        service.list("alice", PageRequest.of(0, 20));

        org.mockito.Mockito.verify(users).searchUsers(eq("123"), eq(123L), any());
        org.mockito.Mockito.verify(users).searchUsers(eq("alice"), eq(0L), any());
    }

    @Test
    void detailEmptyWhenUnknown() {
        when(users.findUserHeader(9L)).thenReturn(Optional.empty());
        assertTrue(service.detail(9L).isEmpty());
    }

    @Test
    void detailComputesLossesAndCategoryAccuracy() {
        UserRowProjection header = projection(5L, 50.0);
        when(users.findUserHeader(5L)).thenReturn(Optional.of(header));
        AnswerRepository.UserCategoryRow category = mock(AnswerRepository.UserCategoryRow.class);
        when(category.getCategory()).thenReturn("science");
        when(category.getAnswered()).thenReturn(4L);
        when(category.getCorrect()).thenReturn(3L);
        when(answers.userCategoryBreakdown(5L)).thenReturn(List.of(category));
        when(answers.recentGames(eq(5L), anyInt())).thenReturn(List.of());
        DuelRepository.DuelRecordRow duel = mock(DuelRepository.DuelRecordRow.class);
        when(duel.getPlayed()).thenReturn(5L);
        when(duel.getWins()).thenReturn(2L);
        when(duel.getDraws()).thenReturn(1L);
        when(duels.duelRecord(5L)).thenReturn(duel);

        UserDetail detail = service.detail(5L).orElseThrow();

        assertEquals(2, detail.duel().losses(), "losses = played - wins - draws");
        assertEquals(75, detail.categories().get(0).accuracyPercent(), "3/4 rounds to 75");
    }

    private static UserRowProjection projection(long id, Double accuracy) {
        UserRowProjection row = mock(UserRowProjection.class);
        when(row.getId()).thenReturn(id);
        when(row.getName()).thenReturn("Name");
        when(row.getUsername()).thenReturn("user");
        when(row.getLanguage()).thenReturn("ru");
        when(row.getGames()).thenReturn(2L);
        when(row.getAccuracy()).thenReturn(accuracy);
        when(row.getElo()).thenReturn(1000);
        when(row.getFirstSeen()).thenReturn(1L);
        when(row.getLastSeen()).thenReturn(2L);
        when(row.getBanned()).thenReturn(false);
        when(row.getBlocked()).thenReturn(false);
        return row;
    }
}
