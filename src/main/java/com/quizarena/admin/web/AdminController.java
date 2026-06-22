package com.quizarena.admin.web;

import com.quizarena.admin.auth.VerifiedAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminController {

    private final AdminStatsService stats;

    public AdminController(AdminStatsService stats) {
        this.stats = stats;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal VerifiedAdmin admin) {
        return new MeResponse(admin.id(), admin.name());
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return stats.snapshot();
    }

    @GetMapping("/stats/overview")
    public OverviewResponse overview() {
        return stats.overview();
    }

    @GetMapping("/stats/answer-distribution")
    public List<CategoryAnswerDistribution> answerDistribution() {
        return stats.answerDistribution();
    }
}
