package com.quizarena.admin.auth;

import com.quizarena.admin.config.AdminPanelProperties;
import com.quizarena.config.TelegramProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the constructor wiring: the verifier has two constructors (Spring one + a test one), so Spring
 * needs the @Autowired marker to pick the right one. Without it the admin context fails to start.
 */
class AdminAuthWiringTest {

    @Test
    void verifierIsInstantiableBySpring() {
        new ApplicationContextRunner()
                .withPropertyValues("admin.panel.enabled=true")
                .withBean(TelegramProperties.class, () -> new TelegramProperties("test:token"))
                .withBean(AdminPanelProperties.class,
                        () -> new AdminPanelProperties(true, List.of(1L), 86400L, false, 0L, "Dev Admin"))
                .withBean(Clock.class, Clock::systemUTC)
                .withBean(TelegramLoginVerifier.class)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(TelegramLoginVerifier.class));
    }
}
