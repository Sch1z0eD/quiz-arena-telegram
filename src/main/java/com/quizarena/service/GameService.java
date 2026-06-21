package com.quizarena.service;

import com.quizarena.bot.GameMessenger;
import com.quizarena.config.GameProperties;
import com.quizarena.domain.AnswerRecord;
import com.quizarena.domain.Category;
import com.quizarena.domain.GameMode;
import com.quizarena.domain.GameResult;
import com.quizarena.domain.GameState;
import com.quizarena.domain.JoinResult;
import com.quizarena.domain.PersonalRank;
import com.quizarena.domain.Profile;
import com.quizarena.domain.Question;
import com.quizarena.domain.RecordResult;
import com.quizarena.domain.Standing;
import com.quizarena.domain.TopScope;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.GameStore;
import com.quizarena.repository.GameStore.Snapshot;
import com.quizarena.repository.QuestionRepository;
import com.quizarena.i18n.Localizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);
    private static final int TOP_LIMIT = 10;

    private final GameStore store;
    private final GameMessenger messenger;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final TaskScheduler scheduler;
    private final GameProperties properties;
    private final Localizer localizer;
    private final LocaleService localeService;
    private final EloService eloService;
    private final AvatarService avatarService;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    public GameService(GameStore store, GameMessenger messenger, QuestionRepository questionRepository,
                       AnswerRepository answerRepository, TaskScheduler scheduler, GameProperties properties,
                       Localizer localizer, LocaleService localeService, EloService eloService,
                       AvatarService avatarService) {
        this.store = store;
        this.messenger = messenger;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.scheduler = scheduler;
        this.properties = properties;
        this.localizer = localizer;
        this.localeService = localeService;
        this.eloService = eloService;
        this.avatarService = avatarService;
    }

    public boolean gameActive(long chatId) {
        return store.gameActive(chatId);
    }

    public boolean hasEnoughQuestions(String category, String difficulty, String language) {
        return questionRepository.countFiltered(category, difficulty, language) >= properties.questionsPerGame();
    }

    public List<Category> availableCategories(String language) {
        List<String> slugs = questionRepository.categoriesWithMinQuestions(language, properties.questionsPerGame());
        List<Category> categories = new ArrayList<>();
        for (Category category : Category.values()) {
            if (slugs.contains(category.slug())) {
                categories.add(category);
            }
        }
        return categories;
    }

    public void startQuiz(long chatId, boolean group, long userId, String name, String category, String difficulty,
                          Locale locale) throws TelegramApiException {
        if (store.gameActive(chatId)) {
            messenger.notice(chatId, localizer.get(locale, "gameAlreadyRunning"));
            return;
        }
        if (!hasEnoughQuestions(category, difficulty, locale.getLanguage())) {
            messenger.notice(chatId, localizer.get(locale, "notEnoughQuestions"));
            return;
        }
        if (group) {
            store.createLobby(chatId, category, difficulty, locale.getLanguage());
            int messageId = messenger.sendLobby(chatId, properties.lobbySeconds(), locale);
            store.setLobbyMessageId(chatId, messageId);
            scheduleLobbyEnd(chatId);
        } else {
            store.startGame(chatId, pickQuestionIds(category, difficulty, locale.getLanguage()),
                    category, difficulty, locale.getLanguage());
            store.joinRoster(chatId, userId, name);
            beginQuestion(chatId, 0);
        }
    }

    public JoinResult join(long chatId, long userId, String name) throws TelegramApiException {
        Snapshot snapshot = store.snapshot(chatId);
        if (snapshot == null || snapshot.state() != GameState.LOBBY) {
            return JoinResult.CLOSED;
        }
        long added = store.joinRoster(chatId, userId, name);
        if (added != 1L) {
            return JoinResult.ALREADY;
        }
        messenger.updateLobby(chatId, store.lobbyMessageId(chatId),
                (int) store.rosterSize(chatId), properties.lobbySeconds(), localeService.parse(snapshot.locale()));
        return JoinResult.JOINED;
    }

    public RecordResult recordAnswer(long chatId, long userId, long token, int option) {
        Snapshot snapshot = store.snapshot(chatId);
        if (snapshot == null || snapshot.state() != GameState.RUNNING) {
            return RecordResult.of(RecordResult.Status.NO_GAME);
        }
        if (!store.isRosterMember(chatId, userId)) {
            return RecordResult.of(RecordResult.Status.NOT_PARTICIPANT);
        }
        long outcome = store.recordAnswer(chatId, snapshot.qIndex(), token, userId);
        if (outcome == -1L) {
            return RecordResult.of(RecordResult.Status.LATE);
        }
        if (outcome == 0L) {
            return RecordResult.of(RecordResult.Status.DUPLICATE);
        }
        boolean correct = option == snapshot.correctOption();
        long points = correct ? speedBonus(snapshot.qStart()) : 0L;
        answerRepository.save(new AnswerRecord(snapshot.gameId(), chatId, userId,
                snapshot.currentQuestionId(), correct, (int) points, System.currentTimeMillis(), GameMode.GAME.name()));
        if (correct) {
            store.incrementScore(chatId, userId, points);
        }
        boolean allAnswered = store.answeredCount(chatId, snapshot.qIndex()) >= store.rosterSize(chatId);
        return new RecordResult(RecordResult.Status.ANSWERED, correct, points, allAnswered);
    }

    public void finishQuestion(long chatId, long token) throws TelegramApiException {
        if (!store.finishRound(chatId, token)) {
            return;
        }
        cancelTimer(chatId);
        Snapshot snapshot = store.snapshot(chatId);
        if (snapshot == null) {
            return;
        }
        Question question = questionRepository.findById(snapshot.currentQuestionId()).orElse(null);
        if (question != null) {
            try {
                messenger.revealAnswer(chatId, snapshot.questionMessageId(), question, localeService.parse(snapshot.locale()));
            } catch (TelegramApiException e) {
                log.warn("Reveal failed in chat {}", chatId, e);
            }
        }
        advance(chatId, snapshot);
    }

    public void showRank(long chatId, long userId, String name, Locale locale) {
        PersonalRank group = store.personal(TopScope.GROUP, chatId, userId);
        PersonalRank global = store.personal(TopScope.GLOBAL, chatId, userId);
        messenger.sendRankCard(chatId, name, group, global, eloService.rating(userId), locale);
    }

    public TopData topData(TopScope scope, long chatId, long userId) {
        return new TopData(store.top(scope, chatId, TOP_LIMIT), store.personal(scope, chatId, userId));
    }

    public Profile profile(long chatId, long userId) {
        long answered = answerRepository.countByUserId(userId);
        long correct = answerRepository.countByUserIdAndCorrectTrue(userId);
        long games = answerRepository.countDistinctGamesByUserId(userId);
        int accuracy = answered == 0 ? 0 : (int) Math.round(correct * 100.0 / answered);
        PersonalRank global = store.personal(TopScope.GLOBAL, chatId, userId);
        long points = global == null ? 0L : global.score();
        Long place = global == null ? null : global.place();
        return new Profile(games, answered, correct, accuracy, points, place, eloService.rating(userId));
    }

    private void advance(long chatId, Snapshot snapshot) throws TelegramApiException {
        int next = snapshot.qIndex() + 1;
        if (next < snapshot.total()) {
            beginQuestion(chatId, next);
            return;
        }
        List<Standing> board = store.scoreboard(chatId);
        Long winnerId = store.gameWinnerId(chatId);
        long gameId = snapshot.gameId();
        store.promoteToLeaderboards(chatId);
        GameResult result = buildResult(chatId, snapshot.category(), board, winnerId, gameId);
        store.cleanup(chatId, snapshot.total());
        messenger.sendResult(chatId, result, localeService.parse(snapshot.locale()));
    }

    private GameResult buildResult(long chatId, String categorySlug, List<Standing> board, Long winnerId, long gameId) {
        if (winnerId == null || board.isEmpty()) {
            return new GameResult(categorySlug, false, "", 0L, 0L, 0L, null, board, chatId < 0, null);
        }
        long answered = answerRepository.countByGameIdAndUserId(gameId, winnerId);
        long correct = answerRepository.countByGameIdAndUserIdAndCorrectTrue(gameId, winnerId);
        PersonalRank global = store.personal(TopScope.GLOBAL, chatId, winnerId);
        Long place = global == null ? null : global.place();
        return new GameResult(categorySlug, true, board.get(0).name(), board.get(0).score(),
                correct, answered, place, board, chatId < 0, avatarService.get(winnerId));
    }

    private void beginQuestion(long chatId, int index) throws TelegramApiException {
        Snapshot snapshot = store.snapshot(chatId);
        long token = store.nextToken();
        Question question = questionRepository.findById(snapshot.questionIds().get(index)).orElseThrow();
        store.beginQuestion(chatId, index, question.getCorrectOption(), token, System.currentTimeMillis());
        int messageId = messenger.sendQuestion(chatId, question, index, snapshot.total(), token,
                localeService.parse(snapshot.locale()));
        store.setQuestionMessageId(chatId, messageId);
        scheduleQuestionTimer(chatId, token);
    }

    private void endLobby(long chatId) throws TelegramApiException {
        timers.remove(chatId);
        Snapshot lobby = store.snapshot(chatId);
        Locale locale = lobby == null ? localeService.parse("") : localeService.parse(lobby.locale());
        if (store.rosterSize(chatId) == 0L) {
            messenger.lobbyCancelled(chatId, store.lobbyMessageId(chatId), locale);
            store.cleanup(chatId, 0);
            return;
        }
        store.startGame(chatId, pickQuestionIds(lobby.category(), lobby.difficulty(), lobby.locale()),
                lobby.category(), lobby.difficulty(), lobby.locale());
        messenger.lobbyStarted(chatId, store.lobbyMessageId(chatId), (int) store.rosterSize(chatId), locale);
        beginQuestion(chatId, 0);
    }

    private List<Long> pickQuestionIds(String category, String difficulty, String language) {
        return questionRepository.findRandomFiltered(category, difficulty, language, properties.questionsPerGame())
                .stream().map(Question::getId).toList();
    }

    private long speedBonus(long questionStartMillis) {
        return Scoring.speedBonus(questionStartMillis, System.currentTimeMillis(),
                properties.questionSeconds() * 1000L, properties.basePoints());
    }

    private void scheduleQuestionTimer(long chatId, long token) {
        schedule(chatId, () -> {
            try {
                finishQuestion(chatId, token);
            } catch (Exception e) {
                log.error("Question timer failed in chat {}", chatId, e);
            }
        }, properties.questionSeconds());
    }

    private void scheduleLobbyEnd(long chatId) {
        schedule(chatId, () -> {
            try {
                endLobby(chatId);
            } catch (Exception e) {
                log.error("Lobby timer failed in chat {}", chatId, e);
            }
        }, properties.lobbySeconds());
    }

    private void schedule(long chatId, Runnable task, int delaySeconds) {
        ScheduledFuture<?> future = scheduler.schedule(task, Instant.now().plusSeconds(delaySeconds));
        ScheduledFuture<?> previous = timers.put(chatId, future);
        if (previous != null) {
            previous.cancel(false);
        }
    }

    private void cancelTimer(long chatId) {
        ScheduledFuture<?> future = timers.remove(chatId);
        if (future != null) {
            future.cancel(false);
        }
    }

    public record TopData(List<Standing> top, PersonalRank personal) {}
}
