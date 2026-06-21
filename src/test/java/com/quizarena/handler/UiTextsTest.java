package com.quizarena.handler;

import com.quizarena.config.BrandProperties;
import com.quizarena.i18n.Localizer;
import com.quizarena.i18n.Plurals;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiTextsTest {

    private final UiTexts texts =
            new UiTexts(new Localizer(messageSource(), new Plurals()), new BrandProperties("ACME Quiz", "@acme"));

    @Test
    void mainMenuTitleUsesConfiguredBrandNotABundleLiteral() {
        String ru = texts.menuHome(Locale.of("ru"));
        String en = texts.menuHome(Locale.of("en"));
        assertTrue(ru.contains("ACME Quiz"), "RU /start title should carry the configured brand");
        assertTrue(en.contains("ACME Quiz"), "EN /start title should carry the configured brand");
        assertFalse(en.contains("QuizArena"), "brand must come from config, not a hardcoded bundle literal");
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
