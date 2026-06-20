package com.quizarena.i18n;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class Localizer {

    private final MessageSource messageSource;
    private final Plurals plurals;

    public Localizer(MessageSource messageSource, Plurals plurals) {
        this.messageSource = messageSource;
        this.plurals = plurals;
    }

    public String get(Locale locale, String key, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }

    public String plural(Locale locale, String nounKey, long count) {
        return messageSource.getMessage(nounKey + "." + plurals.form(locale, count), new Object[]{count}, locale);
    }
}
