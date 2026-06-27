package com.omnixys.cache.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.UUID;

public class ValkeyLockService {

    private static final String RELEASE_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
            "return redis.call('DEL', KEYS[1]) " +
            "else return 0 end";

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> releaseScript;

    public ValkeyLockService(StringRedisTemplate redis) {
        this.redis = redis;
        this.releaseScript = new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class);
    }

    public AcquiredLock acquireLock(String key, long ttlMs) {
        String token = UUID.randomUUID().toString().replace("-", "");
        boolean acquired = Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(key, token, java.time.Duration.ofMillis(ttlMs)));
        return acquired ? new AcquiredLock(key, token) : null;
    }

    public boolean releaseLock(AcquiredLock lock) {
        if (lock == null) return false;
        Long result = redis.execute(releaseScript, List.of(lock.key()), lock.token());
        return Long.valueOf(1).equals(result);
    }

    public record AcquiredLock(String key, String token) {}
}
