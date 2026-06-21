package com.quizarena.admin.auth;

import com.quizarena.admin.config.AdminPanelProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Local development backdoor: logs in a fixed admin without the Telegram widget. Two independent locks are
 * required to exist - the {@code dev} profile AND {@code admin.panel.dev-login=true} (on top of the panel
 * being enabled). With either missing the bean is absent and only real verification can create a session.
 */
@RestController
@RequestMapping("/api/admin/auth")
@Profile("dev")
@ConditionalOnProperty(prefix = "admin.panel", name = {"enabled", "dev-login"}, havingValue = "true")
public class DevLoginController {

    private static final Logger log = LoggerFactory.getLogger(DevLoginController.class);

    private final AdminPanelProperties properties;
    private final AdminLogin adminLogin;

    public DevLoginController(AdminPanelProperties properties, AdminLogin adminLogin) {
        this.properties = properties;
        this.adminLogin = adminLogin;
        log.warn("================================================================");
        log.warn("  DEV ADMIN LOGIN SHIM IS ACTIVE - Telegram verification is");
        log.warn("  BYPASSED (dev profile + admin.panel.dev-login=true).");
        log.warn("  This must NEVER be enabled in production.");
        log.warn("================================================================");
    }

    @GetMapping("/dev-login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void devLogin(HttpServletRequest request, HttpServletResponse response) {
        adminLogin.establish(new VerifiedAdmin(properties.devAdminId(), properties.devAdminName()), request, response);
    }
}
