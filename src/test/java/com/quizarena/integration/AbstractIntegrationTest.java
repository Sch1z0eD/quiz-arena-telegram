package com.quizarena.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared base for every integration test. ALL context-shaping configuration lives here on purpose:
 * the autoconfigure exclude, the {@link MockitoBean} overrides and the property source are identical
 * for every subclass, so Spring caches a single ApplicationContext and Flyway (including the 2014-row
 * V7 seed) runs exactly once for the whole suite. A subclass that adds its own override or property
 * would fork a second context and re-run the migrations.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.autoconfigure.exclude="
                + "org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration")
public abstract class AbstractIntegrationTest {

    // Singleton containers: started once on class load, shared by all tests, reaped by Ryuk at JVM exit.
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    // Telegram I/O and the timer are the only things we cannot exercise for real, so they are mocked here
    // (and only here) to keep one context: the bot makes no network calls and round timers never auto-fire,
    // which lets the lifecycle test drive progression deterministically through the all-answered path.
    @MockitoBean
    TelegramClient telegramClient;
    @MockitoBean
    TaskScheduler taskScheduler;

    @Autowired
    private StringRedisTemplate redis;

    @DynamicPropertySource
    static void wireContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("telegram.bot.token", () -> "test:dummy");
    }

    @BeforeEach
    void flushRedis() {
        redis.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }
}
