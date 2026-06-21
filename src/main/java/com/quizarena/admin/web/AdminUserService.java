package com.quizarena.admin.web;

import com.quizarena.admin.web.UserDetail.CategoryStat;
import com.quizarena.admin.web.UserDetail.DuelSummary;
import com.quizarena.admin.web.UserDetail.RecentGame;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.DuelRepository;
import com.quizarena.repository.UserRepository;
import com.quizarena.repository.UserRepository.UserRowProjection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminUserService {

    private static final int RECENT_GAMES = 10;

    private final UserRepository users;
    private final AnswerRepository answers;
    private final DuelRepository duels;

    public AdminUserService(UserRepository users, AnswerRepository answers, DuelRepository duels) {
        this.users = users;
        this.answers = answers;
        this.duels = duels;
    }

    public PageResponse<UserRow> list(String query, Pageable pageable) {
        String trimmed = query == null ? "" : query.trim();
        long searchId = parseId(trimmed);
        Page<UserRowProjection> page = users.searchUsers(trimmed, searchId, pageable);
        List<UserRow> content = page.getContent().stream().map(AdminUserService::toRow).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public Optional<UserDetail> detail(long id) {
        return users.findUserHeader(id).map(header -> {
            UserRow summary = toRow(header);
            List<CategoryStat> categories = answers.userCategoryBreakdown(id).stream()
                    .map(row -> new CategoryStat(row.getCategory(), row.getAnswered(),
                            accuracy(row.getCorrect(), row.getAnswered())))
                    .toList();
            List<RecentGame> recentGames = answers.recentGames(id, RECENT_GAMES).stream()
                    .map(row -> new RecentGame(row.getGameId(), row.getMode(), row.getFinishedAt(),
                            row.getCorrect(), row.getTotal()))
                    .toList();
            DuelRepository.DuelRecordRow duel = duels.duelRecord(id);
            long losses = duel.getPlayed() - duel.getWins() - duel.getDraws();
            DuelSummary duelSummary = new DuelSummary(duel.getPlayed(), duel.getWins(), duel.getDraws(), losses);
            return new UserDetail(summary, categories, recentGames, duelSummary);
        });
    }

    private static UserRow toRow(UserRowProjection row) {
        Integer accuracy = row.getAccuracy() == null ? null : (int) Math.round(row.getAccuracy());
        return new UserRow(row.getId(), row.getName(), row.getUsername(), row.getLanguage(), row.getGames(),
                accuracy, row.getElo(), row.getFirstSeen(), row.getLastSeen(), row.getBanned(), row.getBlocked());
    }

    private static Integer accuracy(long correct, long answered) {
        return answered == 0 ? null : (int) Math.round(correct * 100.0 / answered);
    }

    // 0 is a sentinel meaning "no id match" (Telegram ids are positive). Non-numeric search keeps name-only.
    private static long parseId(String query) {
        try {
            long value = Long.parseLong(query);
            return value > 0 ? value : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
