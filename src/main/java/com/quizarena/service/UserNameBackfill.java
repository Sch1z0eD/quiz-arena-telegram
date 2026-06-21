package com.quizarena.service;

import com.quizarena.repository.GameStore;
import com.quizarena.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * One-time, idempotent seed of registry display names from the transient leaderboard hash so the admin
 * user list shows names from day one. Only fills rows whose name is still null, so it never clobbers a
 * fresher name written by {@code touch}, and re-running on every startup is harmless. Best-effort.
 */
@Component
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class UserNameBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserNameBackfill.class);

    private final GameStore gameStore;
    private final UserRepository users;

    public UserNameBackfill(GameStore gameStore, UserRepository users) {
        this.gameStore = gameStore;
        this.users = users;
    }

    @Override
    public void run(ApplicationArguments args) {
        Map<Long, String> names = gameStore.allLeaderboardNames();
        int filled = 0;
        for (Map.Entry<Long, String> entry : names.entrySet()) {
            try {
                filled += users.backfillNameIfMissing(entry.getKey(), entry.getValue());
            } catch (RuntimeException e) {
                log.warn("Name backfill failed for user {}", entry.getKey(), e);
            }
        }
        if (filled > 0) {
            log.info("Backfilled names for {} users from the leaderboard hash", filled);
        }
    }
}
