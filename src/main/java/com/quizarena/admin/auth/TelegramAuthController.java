package com.quizarena.admin.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/auth")
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class TelegramAuthController {

    private final TelegramLoginVerifier verifier;
    private final AdminLogin adminLogin;

    public TelegramAuthController(TelegramLoginVerifier verifier, AdminLogin adminLogin) {
        this.verifier = verifier;
        this.adminLogin = adminLogin;
    }

    // Telegram Login Widget redirect target. The query string carries the hash and PII and is never logged.
    @GetMapping("/telegram")
    public void telegram(@RequestParam Map<String, String> params,
                         HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<VerifiedAdmin> admin = verifier.verify(params);
        if (admin.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        adminLogin.establish(admin.get(), request, response);
        response.sendRedirect("/");
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
