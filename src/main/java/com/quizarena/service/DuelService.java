package com.quizarena.service;

import com.quizarena.bot.BotIdentity;
import com.quizarena.bot.DuelMessenger;
import com.quizarena.config.DuelProperties;
import com.quizarena.domain.AnswerRecord;
import com.quizarena.domain.DuelInvite;
import com.quizarena.domain.DuelRecord;
import com.quizarena.domain.DuelResult;
import com.quizarena.domain.Matchup;
import com.quizarena.domain.GameMode;
import com.quizarena.domain.Question;
import com.quizarena.domain.RecordResult;
import com.quizarena.i18n.Localizer;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.DuelRepository;
import com.quizarena.repository.DuelStore;
import com.quizarena.repository.DuelStore.MatchOutcome;
import com.quizarena.repository.DuelStore.Snapshot;
import com.quizarena.repository.GameStore;
import com.quizarena.repository.InviteStore;
import com.quizarena.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class DuelService {

    private static final Logger log = LoggerFactory.getLogger(DuelService.class);

    private final DuelStore store;
    private final DuelMessenger messenger;
    private final GameStore gameStore;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final DuelRepository duelRepository;
    private final TaskScheduler scheduler;
    private final DuelProperties properties;
    private final LocaleService localeService;
    private final EloService eloService;
    private final AvatarService avatarService;
    private final InviteStore inviteStore;
    private final BotIdentity botIdentity;
    private final Localizer localizer;

    private final ConcurrentHashMap<Long, ScheduledFuture<?>> searchTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> duelTimers = new ConcurrentHashMap<>();

    public DuelService(DuelStore store, DuelMessenger messenger, GameStore gameStore,
                       QuestionRepository questionRepository, AnswerRepository answerRepository,
                       DuelRepository duelRepository, TaskScheduler scheduler, DuelProperties properties,
                       LocaleService localeService, EloService eloService, AvatarService avatarService,
                       InviteStore inviteStore, BotIdentity botIdentity, Localizer localizer) {
        this.store = store;
        this.messenger = messenger;
        this.gameStore = gameStore;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.duelRepository = duelRepository;
        this.scheduler = scheduler;
        this.properties = properties;
        this.localeService = localeService;
        this.eloService = eloService;
        this.avatarService = avatarService;
        this.inviteStore = inviteStore;
        this.botIdentity = botIdentity;
        this.localizer = localizer;
    }

    public void search(long chatId, long userId, String name, String category, String difficulty, int messageId,
                       Locale locale) throws TelegramApiException {
        if (gameStore.gameActive(chatId)) {
            messenger.editBusy(chatId, messageId, locale);
            return;
        }
        String bucketCategory = bucket(category);
        String bucketDifficulty = bucket(difficulty);
        MatchOutcome outcome = store.matchOrEnqueue(locale.getLanguage(), bucketCategory, bucketDifficulty,
                userId, chatId, messageId, name, properties.searchSeconds() + 15);
        switch (outcome.status()) {
            case BUSY -> messenger.editBusy(chatId, messageId, locale);
            case QUEUED -> {
                messenger.editSearching(chatId, messageId, bucketCategory, bucketDifficulty, locale);
                scheduleSearchTimeout(userId, chatId, name, messageId, bucketCategory, bucketDifficulty, locale);
            }
            case MATCHED -> startDuel(userId, chatId, name, messageId, locale,
                    outcome.opponentUserId(), outcome.opponentChatId(), outcome.opponentName(),
                    outcome.opponentMessageId(), localeService.resolve(outcome.opponentUserId(), null),
                    category, difficulty);
        }
    }

    public void cancelSearch(long chatId, long userId, String name, String language,
                             String bucketCategory, String bucketDifficulty, int messageId, Locale locale) {
        cancelSearchTimeout(userId);
        if (store.cancelSearch(language, bucketCategory, bucketDifficulty, userId, chatId, messageId, name)) {
            messenger.editCancelled(chatId, messageId, locale);
        }
        // else: already matched - the duel is started by the matcher; stay silent.
    }

    public Optional<Invitation> createInvite(long userId, long chatId, String name,
                                             String category, String difficulty, Locale locale)
            throws TelegramApiException {
        if (gameStore.gameActive(chatId) || store.isBusy(userId)) {
            return Optional.empty();
        }
        DuelInvite invite = new DuelInvite(userId, chatId, locale.getLanguage(),
                "any".equals(category) ? "" : category, "any".equals(difficulty) ? "" : difficulty, name);
        String token = inviteStore.create(invite);
        return Optional.of(new Invitation(token, "https://t.me/" + botIdentity.username() + "?start=duel_" + token));
    }

    // Inline invites carry no chat the bot picked - the inviter's private chat is their own user id.
    // No busy pre-check here: the token may be built on every keystroke; acceptInvite re-checks both sides.
    public Invitation createInlineInvite(long userId, String name, Locale locale) throws TelegramApiException {
        DuelInvite invite = new DuelInvite(userId, userId, locale.getLanguage(), "", "", name);
        String token = inviteStore.create(invite);
        return new Invitation(token, "https://t.me/" + botIdentity.username() + "?start=duel_" + token);
    }

    public void cancelInvite(String token) {
        inviteStore.cancel(token);
    }

    public boolean acceptInvite(String token, long friendUser, long friendChat, String friendName, Locale friendLocale)
            throws TelegramApiException {
        Optional<DuelInvite> claimed = inviteStore.claim(token);
        if (claimed.isEmpty()) {
            messenger.notify(friendChat, localizer.get(friendLocale, "invite.invalid"));
            return false;
        }
        DuelInvite invite = claimed.get();
        if (invite.inviterUserId() == friendUser) {
            messenger.notify(friendChat, localizer.get(friendLocale, "invite.self"));
            return false;
        }
        if (gameStore.gameActive(friendChat) || store.isBusy(friendUser)
                || gameStore.gameActive(invite.inviterChatId()) || store.isBusy(invite.inviterUserId())) {
            messenger.notify(friendChat, localizer.get(friendLocale, "invite.invalid"));
            return false;
        }
        store.markBusy(invite.inviterUserId());
        store.markBusy(friendUser);
        launchInvitedDuel(invite, friendUser, friendChat, friendName, friendLocale);
        return true;
    }

    public RecordResult recordAnswer(long duelId, long userId, long token, int option) {
        Snapshot snapshot = store.snapshot(duelId);
        if (snapshot == null) {
            return RecordResult.of(RecordResult.Status.NO_GAME);
        }
        if (userId != snapshot.userA() && userId != snapshot.userB()) {
            return RecordResult.of(RecordResult.Status.NOT_PARTICIPANT);
        }
        long result = store.recordAnswer(duelId, snapshot.qIndex(), token, userId);
        if (result == -1L) {
            return RecordResult.of(RecordResult.Status.LATE);
        }
        if (result == 0L) {
            return RecordResult.of(RecordResult.Status.DUPLICATE);
        }
        boolean correct = option == snapshot.correctOption();
        long points = correct
                ? Scoring.speedBonus(snapshot.qStart(), System.currentTimeMillis(),
                        properties.questionSeconds() * 1000L, properties.basePoints())
                : 0L;
        long chatId = userId == snapshot.userA() ? snapshot.chatA() : snapshot.chatB();
        answerRepository.save(new AnswerRecord(duelId, chatId, userId, snapshot.currentQuestionId(),
                correct, (int) points, System.currentTimeMillis(), GameMode.DUEL.name()));
        if (correct) {
            store.incrementScore(duelId, userId, points);
        }
        boolean allAnswered = store.answeredCount(duelId, snapshot.qIndex()) >= 2;
        return new RecordResult(RecordResult.Status.ANSWERED, correct, points, allAnswered);
    }

    public void finishQuestion(long duelId, long token) throws TelegramApiException {
        if (!store.finishRound(duelId, token)) {
            return;
        }
        cancelDuelTimer(duelId);
        Snapshot snapshot = store.snapshot(duelId);
        if (snapshot == null) {
            return;
        }
        Question question = questionRepository.findById(snapshot.currentQuestionId()).orElse(null);
        if (question != null) {
            messenger.reveal(snapshot.chatA(), snapshot.questionMessageA(), question, localeService.parse(snapshot.localeA()));
            messenger.reveal(snapshot.chatB(), snapshot.questionMessageB(), question, localeService.parse(snapshot.localeB()));
        }
        advance(duelId, snapshot);
    }

    private void startDuel(long userA, long chatA, String nameA, int messageA, Locale localeA,
                           long userB, long chatB, String nameB, int messageB, Locale localeB,
                           String category, String difficulty) throws TelegramApiException {
        cancelSearchTimeout(userB);
        List<Long> questionIds = questionRepository
                .findRandomFiltered(category, difficulty, localeA.getLanguage(), properties.questionCount())
                .stream().map(Question::getId).toList();
        if (questionIds.isEmpty()) {
            messenger.editFailed(chatA, messageA, localeA);
            messenger.editFailed(chatB, messageB, localeB);
            store.clearBusy(userA);
            store.clearBusy(userB);
            return;
        }
        long duelId = store.nextId();
        store.createDuel(duelId, category, difficulty, userA, chatA, nameA, localeA.getLanguage(),
                userB, chatB, nameB, localeB.getLanguage(), questionIds);
        Matchup matchup = matchup(userA, nameA, userB, nameB, category, difficulty);
        messenger.editMatchup(chatA, messageA, matchup, localeA);
        messenger.editMatchup(chatB, messageB, matchup, localeB);
        beginQuestion(duelId, 0);
    }

    private void launchInvitedDuel(DuelInvite invite, long friendUser, long friendChat, String friendName,
                                   Locale friendLocale) throws TelegramApiException {
        Locale inviterLocale = localeService.parse(invite.locale());
        List<Long> questionIds = questionRepository
                .findRandomFiltered(invite.category(), invite.difficulty(), inviterLocale.getLanguage(),
                        properties.questionCount())
                .stream().map(Question::getId).toList();
        if (questionIds.isEmpty()) {
            store.clearBusy(invite.inviterUserId());
            store.clearBusy(friendUser);
            messenger.notify(invite.inviterChatId(), localizer.get(inviterLocale, "duel.searchFailed"));
            messenger.notify(friendChat, localizer.get(friendLocale, "duel.searchFailed"));
            return;
        }
        long duelId = store.nextId();
        store.createDuel(duelId, invite.category(), invite.difficulty(),
                invite.inviterUserId(), invite.inviterChatId(), invite.inviterName(), inviterLocale.getLanguage(),
                friendUser, friendChat, friendName, friendLocale.getLanguage(), questionIds);
        Matchup matchup = matchup(invite.inviterUserId(), invite.inviterName(),
                friendUser, friendName, invite.category(), invite.difficulty());
        messenger.sendMatchup(invite.inviterChatId(), matchup, inviterLocale);
        messenger.sendMatchup(friendChat, matchup, friendLocale);
        try {
            beginQuestion(duelId, 0);
        } catch (TelegramApiException e) {
            // An inline invite can come from someone who never started the bot, so the first question
            // to the inviter 403s. The token is already claimed and both are marked busy - roll back:
            // free both and tell the friend the opponent is unreachable (not the "invalid link" notice).
            log.warn("Invited duel {} aborted: inviter unreachable", duelId, e);
            store.cleanup(duelId, questionIds.size(), invite.inviterUserId(), friendUser);
            messenger.notify(friendChat, localizer.get(friendLocale, "duel.opponentUnavailable"));
        }
    }

    private void beginQuestion(long duelId, int index) throws TelegramApiException {
        Snapshot snapshot = store.snapshot(duelId);
        long token = store.nextToken();
        Question question = questionRepository.findById(snapshot.questionIds().get(index)).orElseThrow();
        store.beginQuestion(duelId, index, question.getCorrectOption(), token, System.currentTimeMillis());
        int messageA = messenger.sendQuestion(snapshot.chatA(), question, index, snapshot.total(), duelId, token,
                localeService.parse(snapshot.localeA()));
        int messageB = messenger.sendQuestion(snapshot.chatB(), question, index, snapshot.total(), duelId, token,
                localeService.parse(snapshot.localeB()));
        store.setQuestionMessages(duelId, messageA, messageB);
        scheduleDuelTimer(duelId, token);
    }

    private void advance(long duelId, Snapshot snapshot) throws TelegramApiException {
        int next = snapshot.qIndex() + 1;
        if (next < snapshot.total()) {
            beginQuestion(duelId, next);
        } else {
            endDuel(duelId, snapshot);
        }
    }

    private void endDuel(long duelId, Snapshot snapshot) {
        long scoreA = store.score(duelId, snapshot.userA());
        long scoreB = store.score(duelId, snapshot.userB());
        DuelResult.Outcome outcome = scoreA > scoreB ? DuelResult.Outcome.A_WINS
                : scoreB > scoreA ? DuelResult.Outcome.B_WINS : DuelResult.Outcome.DRAW;
        Long winnerId = switch (outcome) {
            case A_WINS -> snapshot.userA();
            case B_WINS -> snapshot.userB();
            case DRAW -> null;
        };
        double eloScoreA = switch (outcome) {
            case A_WINS -> 1.0;
            case B_WINS -> 0.0;
            case DRAW -> 0.5;
        };
        try {
            EloService.EloOutcome elo = eloService.applyDuel(snapshot.userA(), snapshot.userB(), eloScoreA);
            duelRepository.save(new DuelRecord(duelId, snapshot.userA(), snapshot.userB(),
                    (int) scoreA, (int) scoreB, winnerId, snapshot.category(), snapshot.difficulty(),
                    System.currentTimeMillis()));
            DuelResult duelResult = new DuelResult(snapshot.category(), snapshot.nameA(), scoreA,
                    snapshot.nameB(), scoreB, outcome, elo.newA(), elo.deltaA(), elo.newB(), elo.deltaB(),
                    avatarService.get(snapshot.userA()), avatarService.get(snapshot.userB()));
            messenger.sendResult(snapshot.chatA(), localeService.parse(snapshot.localeA()),
                    snapshot.chatB(), localeService.parse(snapshot.localeB()), duelResult);
        } catch (Exception e) {
            log.error("Duel end failed for {}", duelId, e);
        } finally {
            store.cleanup(duelId, snapshot.total(), snapshot.userA(), snapshot.userB());
        }
    }

    private void scheduleDuelTimer(long duelId, long token) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                finishQuestion(duelId, token);
            } catch (Exception e) {
                log.error("Duel timer failed for {}", duelId, e);
            }
        }, Instant.now().plusSeconds(properties.questionSeconds()));
        ScheduledFuture<?> previous = duelTimers.put(duelId, future);
        if (previous != null) {
            previous.cancel(false);
        }
    }

    private void cancelDuelTimer(long duelId) {
        ScheduledFuture<?> future = duelTimers.remove(duelId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void scheduleSearchTimeout(long userId, long chatId, String name, int messageId,
                                       String bucketCategory, String bucketDifficulty, Locale locale) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            searchTimers.remove(userId);
            if (store.cancelSearch(locale.getLanguage(), bucketCategory, bucketDifficulty, userId, chatId, messageId, name)) {
                messenger.editFailed(chatId, messageId, locale);
            }
        }, Instant.now().plusSeconds(properties.searchSeconds()));
        ScheduledFuture<?> previous = searchTimers.put(userId, future);
        if (previous != null) {
            previous.cancel(false);
        }
    }

    private void cancelSearchTimeout(long userId) {
        ScheduledFuture<?> future = searchTimers.remove(userId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private Matchup matchup(long userA, String nameA, long userB, String nameB, String category, String difficulty) {
        return new Matchup(nameA, eloService.rating(userA), avatarService.get(userA),
                nameB, eloService.rating(userB), avatarService.get(userB), category, difficulty);
    }

    private static String bucket(String filter) {
        return filter.isEmpty() ? "any" : filter;
    }

    public record Invitation(String token, String link) {}
}
