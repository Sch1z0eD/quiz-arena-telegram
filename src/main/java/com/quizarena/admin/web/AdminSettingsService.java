package com.quizarena.admin.web;

import com.quizarena.admin.audit.AuditService;
import com.quizarena.admin.auth.VerifiedAdmin;
import com.quizarena.service.GameSettings;
import com.quizarena.service.GameSettingsSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminSettingsService {

    private final GameSettings settings;
    private final AuditService audit;

    public AdminSettingsService(GameSettings settings, AuditService audit) {
        this.settings = settings;
        this.audit = audit;
    }

    public GameSettingsSnapshot get() {
        return settings.snapshot();
    }

    public GameSettingsSnapshot update(VerifiedAdmin admin, GameSettingsSnapshot values) {
        GameSettingsSnapshot saved = settings.update(values);
        audit.record(admin, "settings.updated", null, describe(saved));
        return saved;
    }

    private static String describe(GameSettingsSnapshot s) {
        return "questionsPerGame=" + s.questionsPerGame() + " questionSeconds=" + s.questionSeconds()
                + " basePoints=" + s.basePoints() + " lobbySeconds=" + s.lobbySeconds()
                + " duelSearchSeconds=" + s.duelSearchSeconds() + " duelQuestionSeconds=" + s.duelQuestionSeconds()
                + " duelQuestionCount=" + s.duelQuestionCount() + " duelBasePoints=" + s.duelBasePoints();
    }
}
