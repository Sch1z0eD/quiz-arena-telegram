package com.quizarena.service;

import com.quizarena.domain.UserSettings;
import com.quizarena.repository.UserSettingsRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Service
public class LocaleService {

    private static final Locale DEFAULT_LOCALE = Locale.of("en");
    private static final String DEFAULT_LANGUAGE = "en";
    private static final Duration CACHE_TTL = Duration.ofDays(1);

    private final UserSettingsRepository repository;
    private final StringRedisTemplate redis;
    private final LanguageRegistry registry;

    public LocaleService(UserSettingsRepository repository, StringRedisTemplate redis, LanguageRegistry registry) {
        this.repository = repository;
        this.redis = redis;
        this.registry = registry;
    }

    public Locale resolve(long userId, String telegramLanguageCode) {
        String cached = redis.opsForValue().get(key(userId));
        if (cached != null) {
            return parse(cached);
        }
        Optional<UserSettings> stored = repository.findById(userId);
        String language = stored.map(UserSettings::getLanguage)
                .orElse(defaultLanguage(telegramLanguageCode));
        redis.opsForValue().set(key(userId), language, CACHE_TTL);
        return parse(language);
    }

    public void setLanguage(long userId, String language) {
        repository.save(new UserSettings(userId, language));
        redis.opsForValue().set(key(userId), language, CACHE_TTL);
    }

    public Locale parse(String language) {
        return language == null || language.isBlank() ? DEFAULT_LOCALE : Locale.of(language);
    }

    // The user's Telegram language wins only if we actually offer it; otherwise fall back to the default.
    private String defaultLanguage(String telegramLanguageCode) {
        if (telegramLanguageCode != null) {
            String base = telegramLanguageCode.toLowerCase().split("[-_]", 2)[0];
            if (registry.isEnabled(base)) {
                return base;
            }
        }
        return DEFAULT_LANGUAGE;
    }

    private static String key(long userId) {
        return "user:" + userId + ":lang";
    }
}
