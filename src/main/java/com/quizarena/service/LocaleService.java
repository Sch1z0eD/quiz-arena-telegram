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

    private static final Locale RU = Locale.of("ru");
    private static final Locale EN = Locale.of("en");
    private static final Duration CACHE_TTL = Duration.ofDays(1);

    private final UserSettingsRepository repository;
    private final StringRedisTemplate redis;

    public LocaleService(UserSettingsRepository repository, StringRedisTemplate redis) {
        this.repository = repository;
        this.redis = redis;
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
        return "ru".equals(language) ? RU : EN;
    }

    private static String defaultLanguage(String telegramLanguageCode) {
        return telegramLanguageCode != null && telegramLanguageCode.toLowerCase().startsWith("ru") ? "ru" : "en";
    }

    private static String key(long userId) {
        return "user:" + userId + ":lang";
    }
}
