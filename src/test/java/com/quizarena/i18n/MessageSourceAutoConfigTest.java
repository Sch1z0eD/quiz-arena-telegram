package com.quizarena.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MessageSourceAutoConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MessageSourceAutoConfiguration.class))
            .withPropertyValues(
                    "spring.messages.basename=messages",
                    "spring.messages.encoding=UTF-8",
                    "spring.messages.fallback-to-system-locale=false");

    @Test
    void bootRegistersResourceBundleMessageSourceFromBaseBundle() {
        runner.run(context -> {
            MessageSource messageSource = context.getBean(MessageSource.class);
            assertThat(messageSource)
                    .as("auto-config must build a real bundle source, not the empty DelegatingMessageSource "
                            + "(requires a base messages.properties on the classpath)")
                    .isInstanceOf(ResourceBundleMessageSource.class);
            assertThat(messageSource.getMessage("menu.home", null, Locale.of("ru"))).isNotBlank();
            assertThat(messageSource.getMessage("menu.home", null, Locale.of("en"))).isNotBlank();
        });
    }
}
