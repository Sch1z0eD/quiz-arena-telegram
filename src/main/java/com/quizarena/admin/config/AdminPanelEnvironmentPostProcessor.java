package com.quizarena.admin.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Turns the app into a servlet web app only when {@code admin.panel.enabled=true}. Otherwise the property
 * is left untouched: the bot stays non-web (see application.properties), and Spring Security auto-config is
 * web-conditional, so it stays inert with no admin beans loaded.
 */
public class AdminPanelEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = Binder.get(environment).bind("admin.panel.enabled", Boolean.class).orElse(false);
        if (enabled) {
            environment.getPropertySources().addFirst(new MapPropertySource(
                    "adminPanel", Map.of("spring.main.web-application-type", "servlet")));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
