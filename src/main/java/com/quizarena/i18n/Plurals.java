package com.quizarena.i18n;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class Plurals {

    public String form(Locale locale, long count) {
        if ("ru".equals(locale.getLanguage())) {
            long mod10 = Math.floorMod(count, 10);
            long mod100 = Math.floorMod(count, 100);
            if (mod10 == 1 && mod100 != 11) {
                return "one";
            }
            if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
                return "few";
            }
            return "many";
        }
        return count == 1 ? "one" : "other";
    }
}
