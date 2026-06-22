package com.quizarena.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Global token bucket over Redis, used to cap broadcast send rate well under Telegram limits.
 */
@Component
public class RateLimiter {

    private static final int RATE_PER_SECOND = 25;
    private static final int CAPACITY = 25;
    private static final long RETRY_SLEEP_MS = 25;

    private final StringRedisTemplate redis;
    private final RedisScript<Long> tokenBucketScript;

    public RateLimiter(StringRedisTemplate redis, @Qualifier("tokenBucketScript") RedisScript<Long> tokenBucketScript) {
        this.redis = redis;
        this.tokenBucketScript = tokenBucketScript;
    }

    /** Blocks until a token is available for the given bucket. Returns false if interrupted. */
    public boolean acquire(String bucketKey) {
        while (!tryAcquire(bucketKey)) {
            try {
                Thread.sleep(RETRY_SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    private boolean tryAcquire(String bucketKey) {
        Long allowed = redis.execute(tokenBucketScript, List.of(bucketKey),
                Long.toString(System.currentTimeMillis()), Integer.toString(RATE_PER_SECOND), Integer.toString(CAPACITY));
        return allowed != null && allowed == 1L;
    }
}
