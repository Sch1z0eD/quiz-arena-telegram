package com.quizarena.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisConfig {

    @Bean
    public RedisScript<Long> recordAnswerScript() {
        return RedisScript.of(new ClassPathResource("redis/record_answer.lua"), Long.class);
    }

    @Bean
    public RedisScript<Long> finishRoundScript() {
        return RedisScript.of(new ClassPathResource("redis/finish_round.lua"), Long.class);
    }

    @Bean
    public RedisScript<String> matchOrEnqueueScript() {
        return RedisScript.of(new ClassPathResource("redis/match_or_enqueue.lua"), String.class);
    }

    @Bean
    public RedisScript<Long> cancelSearchScript() {
        return RedisScript.of(new ClassPathResource("redis/cancel_search.lua"), Long.class);
    }
}
