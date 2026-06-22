package com.quizarena.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

// A language can be enabled in the registry before its bundle ships; rendering must fall back to the base
// (English) bundle rather than throw. This mirrors the production MessageSource configuration.
class LocalizerFallbackTest {

    private final Localizer localizer = new Localizer(messageSource(), new Plurals());

    @Test
    void unknownLocaleFallsBackToBaseBundle() {
        assertEquals("Play", localizer.get(Locale.of("en"), "btn.play"), "base bundle is English");
        assertEquals("Играть", localizer.get(Locale.of("ru"), "btn.play"), "ru bundle overrides");
        assertEquals("Play", localizer.get(Locale.of("de"), "btn.play"), "no de bundle: falls back to base, no exception");
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
