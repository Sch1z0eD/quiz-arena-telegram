package com.quizarena.repository;

import com.quizarena.config.GameProperties;
import com.quizarena.domain.GameState;
import com.quizarena.domain.OptionOrder;
import com.quizarena.domain.PersonalRank;
import com.quizarena.domain.Standing;
import com.quizarena.domain.TopScope;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class GameStore {

    private static final String TOKEN_SEQ = "quizarena:token";
    private static final String GAME_SEQ = "quizarena:gameId";
    private static final String LB_NAMES = "lb:names";
    private static final Duration WEEKLY_TTL = Duration.ofDays(14);

    private final StringRedisTemplate redis;
    private final RedisScript<Long> recordAnswerScript;
    private final RedisScript<Long> finishRoundScript;
    private final Duration ttl;

    public GameStore(StringRedisTemplate redis,
                     @Qualifier("recordAnswerScript") RedisScript<Long> recordAnswerScript,
                     @Qualifier("finishRoundScript") RedisScript<Long> finishRoundScript,
                     GameProperties properties) {
        this.redis = redis;
        this.recordAnswerScript = recordAnswerScript;
        this.finishRoundScript = finishRoundScript;
        this.ttl = properties.ttl();
    }

    public long nextToken() {
        Long value = redis.opsForValue().increment(TOKEN_SEQ);
        return value == null ? 0L : value;
    }

    public boolean gameActive(long chatId) {
        return Boolean.TRUE.equals(redis.hasKey(gameKey(chatId)));
    }

    // Reads the matchmaking busy flag written by DuelStore (shared Redis key) for the solo-vs-duel
    // mutual exclusion: a private chat must not run a solo game while the user is in a duel/search.
    public boolean isDuelBusy(long userId) {
        return Boolean.TRUE.equals(redis.hasKey("mm:busy:" + userId));
    }

    public GameState state(long chatId) {
        String value = hash().get(gameKey(chatId), "state");
        return value == null ? null : GameState.valueOf(value);
    }

    public void createLobby(long chatId, String category, String difficulty, String locale) {
        hash().putAll(gameKey(chatId), Map.of(
                "state", GameState.LOBBY.name(),
                "cat", category,
                "diff", difficulty,
                "locale", locale));
        touch(gameKey(chatId));
    }

    public void setLobbyMessageId(long chatId, int messageId) {
        hash().put(gameKey(chatId), "lobbyMsgId", Integer.toString(messageId));
        touch(gameKey(chatId));
    }

    public int lobbyMessageId(long chatId) {
        return readInt(hash().get(gameKey(chatId), "lobbyMsgId"), 0);
    }

    public long joinRoster(long chatId, long userId, String name) {
        Long added = redis.opsForSet().add(rosterKey(chatId), Long.toString(userId));
        hash().put(namesKey(chatId), Long.toString(userId), name);
        touch(rosterKey(chatId));
        touch(namesKey(chatId));
        return added == null ? 0L : added;
    }

    public long rosterSize(long chatId) {
        Long size = redis.opsForSet().size(rosterKey(chatId));
        return size == null ? 0L : size;
    }

    public boolean isRosterMember(long chatId, long userId) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(rosterKey(chatId), Long.toString(userId)));
    }

    public void startGame(long chatId, List<Long> questionIds, String category, String difficulty, String locale) {
        String ids = String.join(",", questionIds.stream().map(String::valueOf).toList());
        hash().putAll(gameKey(chatId), Map.of(
                "state", GameState.RUNNING.name(),
                "qIds", ids,
                "total", Integer.toString(questionIds.size()),
                "qIndex", "-1",
                "gameId", Long.toString(nextGameId()),
                "cat", category,
                "diff", difficulty,
                "locale", locale));
        touch(gameKey(chatId));
    }

    private long nextGameId() {
        Long value = redis.opsForValue().increment(GAME_SEQ);
        return value == null ? 0L : value;
    }

    public void beginQuestion(long chatId, int index, int correctOption, OptionOrder order, long token, long startMillis) {
        hash().putAll(gameKey(chatId), Map.of(
                "qIndex", Integer.toString(index),
                "qCorrect", Integer.toString(correctOption),
                "qOrder", order.toCsv(),
                "qStart", Long.toString(startMillis)));
        touch(gameKey(chatId));
        redis.opsForValue().set(roundKey(chatId), Long.toString(token), ttl);
    }

    public void setQuestionMessageId(long chatId, int messageId) {
        hash().put(gameKey(chatId), "qMsgId", Integer.toString(messageId));
        touch(gameKey(chatId));
    }

    public long recordAnswer(long chatId, int qIndex, long token, long userId) {
        Long result = redis.execute(recordAnswerScript,
                List.of(roundKey(chatId), answeredKey(chatId, qIndex)),
                Long.toString(token), Long.toString(userId), Long.toString(ttl.toSeconds()));
        return result == null ? -1L : result;
    }

    public boolean finishRound(long chatId, long token) {
        Long result = redis.execute(finishRoundScript, List.of(roundKey(chatId)), Long.toString(token));
        return result != null && result == 1L;
    }

    public long answeredCount(long chatId, int qIndex) {
        Long size = redis.opsForSet().size(answeredKey(chatId, qIndex));
        return size == null ? 0L : size;
    }

    public void incrementScore(long chatId, long userId, long points) {
        redis.opsForZSet().incrementScore(scoresKey(chatId), Long.toString(userId), points);
        touch(scoresKey(chatId));
    }

    public Snapshot snapshot(long chatId) {
        if (!gameActive(chatId)) {
            return null;
        }
        List<String> values = hash().multiGet(gameKey(chatId),
                List.of("state", "qIndex", "total", "qCorrect", "qStart", "qMsgId", "qIds", "cat", "diff", "gameId", "locale", "qOrder"));
        String state = values.get(0);
        if (state == null) {
            return null;
        }
        return new Snapshot(
                GameState.valueOf(state),
                readInt(values.get(1), -1),
                readInt(values.get(2), 0),
                readInt(values.get(3), -1),
                readLong(values.get(4), 0L),
                readInt(values.get(5), 0),
                parseIds(values.get(6)),
                orEmpty(values.get(7)),
                orEmpty(values.get(8)),
                readLong(values.get(9), 0L),
                orEmpty(values.get(10)),
                OptionOrder.parse(values.get(11)));
    }

    public List<Standing> scoreboard(long chatId) {
        return toStandings(redis.opsForZSet().reverseRangeWithScores(scoresKey(chatId), 0, -1),
                hash().entries(namesKey(chatId)));
    }

    public Long gameWinnerId(long chatId) {
        Set<String> top = redis.opsForZSet().reverseRange(scoresKey(chatId), 0, 0);
        if (top == null || top.isEmpty()) {
            return null;
        }
        return Long.parseLong(top.iterator().next());
    }

    public void promoteToLeaderboards(long chatId) {
        var tuples = redis.opsForZSet().reverseRangeWithScores(scoresKey(chatId), 0, -1);
        if (tuples == null || tuples.isEmpty()) {
            return;
        }
        Map<String, String> names = hash().entries(namesKey(chatId));
        String iso = isoWeek();
        String weeklyGlobal = lbWeeklyGlobal(iso);
        String weeklyGroup = lbWeeklyGroup(chatId, iso);
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userId = tuple.getValue();
            double score = tuple.getScore() == null ? 0 : tuple.getScore();
            redis.opsForZSet().incrementScore(lbGlobal(), userId, score);
            redis.opsForZSet().incrementScore(lbGroup(chatId), userId, score);
            redis.opsForZSet().incrementScore(weeklyGlobal, userId, score);
            redis.opsForZSet().incrementScore(weeklyGroup, userId, score);
            String name = names.get(userId);
            if (name != null) {
                hash().put(LB_NAMES, userId, name);
            }
        }
        redis.expire(weeklyGlobal, WEEKLY_TTL);
        redis.expire(weeklyGroup, WEEKLY_TTL);
    }

    public List<Standing> top(TopScope scope, long chatId, int limit) {
        String key = leaderboardKey(scope, chatId);
        return toStandings(redis.opsForZSet().reverseRangeWithScores(key, 0, limit - 1), hash().entries(LB_NAMES));
    }

    public PersonalRank personal(TopScope scope, long chatId, long userId) {
        return personalForKey(leaderboardKey(scope, chatId), userId);
    }

    public PersonalRank personalWeeklyGlobal(long userId) {
        return personalForKey(lbWeeklyGlobal(isoWeek()), userId);
    }

    private PersonalRank personalForKey(String key, long userId) {
        String member = Long.toString(userId);
        Double score = redis.opsForZSet().score(key, member);
        if (score == null) {
            return null;
        }
        Long rank = redis.opsForZSet().reverseRank(key, member);
        return new PersonalRank((long) (double) score, rank == null ? 0 : rank + 1);
    }

    public void cleanup(long chatId, int total) {
        List<String> keys = new ArrayList<>(List.of(
                gameKey(chatId), rosterKey(chatId), scoresKey(chatId), namesKey(chatId), roundKey(chatId)));
        for (int i = 0; i < total; i++) {
            keys.add(answeredKey(chatId, i));
        }
        redis.delete(keys);
    }

    private List<Standing> toStandings(java.util.Set<ZSetOperations.TypedTuple<String>> tuples,
                                       Map<String, String> names) {
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }
        List<Standing> standings = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userId = tuple.getValue();
            double score = tuple.getScore() == null ? 0 : tuple.getScore();
            standings.add(new Standing(names.getOrDefault(userId, userId), (long) score));
        }
        return standings;
    }

    private void touch(String key) {
        redis.expire(key, ttl);
    }

    private HashOperations<String, String, String> hash() {
        return redis.opsForHash();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static int readInt(String value, int fallback) {
        return value == null ? fallback : Integer.parseInt(value);
    }

    private static long readLong(String value, long fallback) {
        return value == null ? fallback : Long.parseLong(value);
    }

    private static List<Long> parseIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : csv.split(",")) {
            ids.add(Long.parseLong(part));
        }
        return ids;
    }

    private static String isoWeek() {
        LocalDate now = LocalDate.now();
        return "%d-W%02d".formatted(now.get(IsoFields.WEEK_BASED_YEAR), now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    private static String leaderboardKey(TopScope scope, long chatId) {
        return switch (scope) {
            case GROUP -> lbGroup(chatId);
            case GLOBAL -> lbGlobal();
            case WEEK -> lbWeeklyGroup(chatId, isoWeek());
        };
    }

    private static String gameKey(long chatId) {
        return "game:" + chatId;
    }

    private static String rosterKey(long chatId) {
        return "game:" + chatId + ":roster";
    }

    private static String answeredKey(long chatId, int qIndex) {
        return "game:" + chatId + ":answered:" + qIndex;
    }

    private static String scoresKey(long chatId) {
        return "game:" + chatId + ":scores";
    }

    private static String namesKey(long chatId) {
        return "game:" + chatId + ":names";
    }

    private static String roundKey(long chatId) {
        return "game:" + chatId + ":round";
    }

    private static String lbGlobal() {
        return "lb:global";
    }

    private static String lbGroup(long chatId) {
        return "lb:group:" + chatId;
    }

    private static String lbWeeklyGlobal(String iso) {
        return "lb:weekly:global:" + iso;
    }

    private static String lbWeeklyGroup(long chatId, String iso) {
        return "lb:weekly:group:" + chatId + ":" + iso;
    }

    public record Snapshot(
            GameState state,
            int qIndex,
            int total,
            int correctOption,
            long qStart,
            int questionMessageId,
            List<Long> questionIds,
            String category,
            String difficulty,
            long gameId,
            String locale,
            OptionOrder order) {

        public long currentQuestionId() {
            return questionIds.get(qIndex);
        }
    }
}
