package com.quizarena.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimiterIT extends AbstractIntegrationTest {

    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    @Qualifier("tokenBucketScript")
    private RedisScript<Long> tokenBucketScript;

    @Test
    void bucketAllowsCapacityAtAnInstantThenRefillsOverTime() {
        String key = "broadcast:bucket:itest";
        long now = 1_700_000_000_000L;

        int allowed = 0;
        for (int i = 0; i < 40; i++) {
            allowed += consume(key, now); // same instant: no refill
        }
        assertEquals(25, allowed, "capacity caps bursts to 25 at a single instant");

        assertEquals(0, consume(key, now), "bucket is empty until time passes");
        assertEquals(1, consume(key, now + 1000), "~25 tokens refill after a second, so the next is allowed");
    }

    private int consume(String key, long now) {
        Long allowed = redis.execute(tokenBucketScript, List.of(key),
                Long.toString(now), "25", "25");
        return allowed == null ? 0 : allowed.intValue();
    }
}
