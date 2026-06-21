package com.quizarena.repository;

import com.quizarena.config.GameProperties;
import com.quizarena.domain.OptionOrder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Repository
public class DuelStore {

    // Shared id counter with GameStore so duelId and gameId never collide in answers.game_id.
    private static final String ID_SEQ = "quizarena:gameId";
    private static final Duration BUSY_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;
    private final RedisScript<Long> recordAnswerScript;
    private final RedisScript<Long> finishRoundScript;
    private final RedisScript<String> matchOrEnqueueScript;
    private final RedisScript<Long> cancelSearchScript;
    private final Duration ttl;

    public DuelStore(StringRedisTemplate redis,
                     @Qualifier("recordAnswerScript") RedisScript<Long> recordAnswerScript,
                     @Qualifier("finishRoundScript") RedisScript<Long> finishRoundScript,
                     @Qualifier("matchOrEnqueueScript") RedisScript<String> matchOrEnqueueScript,
                     @Qualifier("cancelSearchScript") RedisScript<Long> cancelSearchScript,
                     GameProperties properties) {
        this.redis = redis;
        this.recordAnswerScript = recordAnswerScript;
        this.finishRoundScript = finishRoundScript;
        this.matchOrEnqueueScript = matchOrEnqueueScript;
        this.cancelSearchScript = cancelSearchScript;
        this.ttl = properties.ttl();
    }

    public long nextId() {
        Long value = redis.opsForValue().increment(ID_SEQ);
        return value == null ? 0L : value;
    }

    public long nextToken() {
        Long value = redis.opsForValue().increment("quizarena:token");
        return value == null ? 0L : value;
    }

    public boolean isBusy(long userId) {
        return Boolean.TRUE.equals(redis.hasKey(busyKey(userId)));
    }

    public MatchOutcome matchOrEnqueue(String language, String bucketCategory, String bucketDifficulty,
                                       long userId, long chatId, int messageId, String name, int queueTtlSeconds) {
        String result = redis.execute(matchOrEnqueueScript,
                List.of(queueKey(language, bucketCategory, bucketDifficulty), busyKey(userId)),
                entry(userId, chatId, messageId, name), Long.toString(BUSY_TTL.toSeconds()),
                Integer.toString(queueTtlSeconds));
        return MatchOutcome.parse(result);
    }

    public boolean cancelSearch(String language, String bucketCategory, String bucketDifficulty,
                                long userId, long chatId, int messageId, String name) {
        Long result = redis.execute(cancelSearchScript,
                List.of(queueKey(language, bucketCategory, bucketDifficulty), busyKey(userId)),
                entry(userId, chatId, messageId, name));
        return result != null && result == 1L;
    }

    public void clearBusy(long userId) {
        redis.delete(busyKey(userId));
    }

    public void markBusy(long userId) {
        redis.opsForValue().set(busyKey(userId), "1", BUSY_TTL);
    }

    public void createDuel(long duelId, String category, String difficulty,
                           long userA, long chatA, String nameA, String localeA,
                           long userB, long chatB, String nameB, String localeB,
                           List<Long> questionIds) {
        String ids = String.join(",", questionIds.stream().map(String::valueOf).toList());
        Map<String, String> fields = new java.util.HashMap<>();
        fields.put("qIds", ids);
        fields.put("total", Integer.toString(questionIds.size()));
        fields.put("qIndex", "-1");
        fields.put("cat", category);
        fields.put("diff", difficulty);
        fields.put("userA", Long.toString(userA));
        fields.put("chatA", Long.toString(chatA));
        fields.put("nameA", nameA);
        fields.put("localeA", localeA);
        fields.put("userB", Long.toString(userB));
        fields.put("chatB", Long.toString(chatB));
        fields.put("nameB", nameB);
        fields.put("localeB", localeB);
        hash().putAll(duelKey(duelId), fields);
        redis.expire(duelKey(duelId), ttl);
    }

    public void beginQuestion(long duelId, int index, int correctOption, OptionOrder order, long token, long startMillis) {
        hash().putAll(duelKey(duelId), Map.of(
                "qIndex", Integer.toString(index),
                "qCorrect", Integer.toString(correctOption),
                "qOrder", order.toCsv(),
                "qStart", Long.toString(startMillis)));
        redis.expire(duelKey(duelId), ttl);
        redis.opsForValue().set(roundKey(duelId), Long.toString(token), ttl);
    }

    public void setQuestionMessages(long duelId, int messageIdA, int messageIdB) {
        hash().putAll(duelKey(duelId), Map.of(
                "qMsgA", Integer.toString(messageIdA),
                "qMsgB", Integer.toString(messageIdB)));
        redis.expire(duelKey(duelId), ttl);
    }

    public long recordAnswer(long duelId, int qIndex, long token, long userId) {
        Long result = redis.execute(recordAnswerScript,
                List.of(roundKey(duelId), answeredKey(duelId, qIndex)),
                Long.toString(token), Long.toString(userId), Long.toString(ttl.toSeconds()));
        return result == null ? -1L : result;
    }

    public boolean finishRound(long duelId, long token) {
        Long result = redis.execute(finishRoundScript, List.of(roundKey(duelId)), Long.toString(token));
        return result != null && result == 1L;
    }

    public long answeredCount(long duelId, int qIndex) {
        Long size = redis.opsForSet().size(answeredKey(duelId, qIndex));
        return size == null ? 0L : size;
    }

    public void incrementScore(long duelId, long userId, long points) {
        redis.opsForZSet().incrementScore(scoresKey(duelId), Long.toString(userId), points);
        redis.expire(scoresKey(duelId), ttl);
    }

    public long score(long duelId, long userId) {
        Double value = redis.opsForZSet().score(scoresKey(duelId), Long.toString(userId));
        return value == null ? 0L : (long) (double) value;
    }

    public Snapshot snapshot(long duelId) {
        if (!Boolean.TRUE.equals(redis.hasKey(duelKey(duelId)))) {
            return null;
        }
        List<String> v = hash().multiGet(duelKey(duelId), List.of(
                "qIndex", "total", "qCorrect", "qStart", "qIds",
                "chatA", "chatB", "userA", "userB", "qMsgA", "qMsgB", "cat", "diff", "nameA", "nameB",
                "localeA", "localeB", "qOrder"));
        if (v.get(4) == null) {
            return null;
        }
        return new Snapshot(
                readInt(v.get(0), -1), readInt(v.get(1), 0), readInt(v.get(2), -1), readLong(v.get(3), 0L),
                parseIds(v.get(4)),
                readLong(v.get(5), 0L), readLong(v.get(6), 0L), readLong(v.get(7), 0L), readLong(v.get(8), 0L),
                readInt(v.get(9), 0), readInt(v.get(10), 0), orEmpty(v.get(11)), orEmpty(v.get(12)),
                orEmpty(v.get(13)), orEmpty(v.get(14)), orEmpty(v.get(15)), orEmpty(v.get(16)),
                OptionOrder.parse(v.get(17)));
    }

    public void cleanup(long duelId, int total, long userA, long userB) {
        List<String> keys = new ArrayList<>(List.of(
                duelKey(duelId), roundKey(duelId), scoresKey(duelId), busyKey(userA), busyKey(userB)));
        for (int i = 0; i < total; i++) {
            keys.add(answeredKey(duelId, i));
        }
        redis.delete(keys);
    }

    private HashOperations<String, String, String> hash() {
        return redis.opsForHash();
    }

    private static String entry(long userId, long chatId, int messageId, String name) {
        String encoded = Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8));
        return userId + ":" + chatId + ":" + messageId + ":" + encoded;
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

    // Language is the first segment and a hard wall: en and ru searchers never share a queue.
    static String queueKey(String language, String category, String difficulty) {
        return "mm:" + language + ":" + category + ":" + difficulty;
    }

    private static String busyKey(long userId) {
        return "mm:busy:" + userId;
    }

    private static String duelKey(long duelId) {
        return "duel:" + duelId;
    }

    private static String answeredKey(long duelId, int qIndex) {
        return "duel:" + duelId + ":answered:" + qIndex;
    }

    private static String roundKey(long duelId) {
        return "duel:" + duelId + ":round";
    }

    private static String scoresKey(long duelId) {
        return "duel:" + duelId + ":scores";
    }

    public record MatchOutcome(Status status, long opponentUserId, long opponentChatId,
                               int opponentMessageId, String opponentName) {

        public enum Status {
            BUSY,
            QUEUED,
            MATCHED
        }

        static MatchOutcome parse(String raw) {
            if (raw == null) {
                return new MatchOutcome(Status.QUEUED, 0L, 0L, 0, "");
            }
            if (raw.equals("BUSY")) {
                return new MatchOutcome(Status.BUSY, 0L, 0L, 0, "");
            }
            if (raw.equals("QUEUED")) {
                return new MatchOutcome(Status.QUEUED, 0L, 0L, 0, "");
            }
            String[] parts = raw.substring("MATCH:".length()).split(":");
            String name = new String(Base64.getDecoder().decode(parts[3]), StandardCharsets.UTF_8);
            return new MatchOutcome(Status.MATCHED, Long.parseLong(parts[0]), Long.parseLong(parts[1]),
                    Integer.parseInt(parts[2]), name);
        }
    }

    public record Snapshot(
            int qIndex,
            int total,
            int correctOption,
            long qStart,
            List<Long> questionIds,
            long chatA,
            long chatB,
            long userA,
            long userB,
            int questionMessageA,
            int questionMessageB,
            String category,
            String difficulty,
            String nameA,
            String nameB,
            String localeA,
            String localeB,
            OptionOrder order) {

        public long currentQuestionId() {
            return questionIds.get(qIndex);
        }
    }
}
