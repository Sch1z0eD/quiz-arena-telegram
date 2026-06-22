package com.quizarena.admin.web;

import com.quizarena.admin.auth.VerifiedAdmin;
import com.quizarena.service.GameSettingsSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminSettingsController {

    private final AdminSettingsService service;

    public AdminSettingsController(AdminSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public GameSettingsSnapshot get() {
        return service.get();
    }

    @PutMapping
    public GameSettingsSnapshot update(@AuthenticationPrincipal VerifiedAdmin admin,
                                       @RequestBody GameSettingsSnapshot body) {
        return service.update(admin, body);
    }
}
