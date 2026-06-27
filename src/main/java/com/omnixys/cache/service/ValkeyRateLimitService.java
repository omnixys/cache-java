package com.omnixys.cache.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

public class ValkeyRateLimitService {

    private static final String ATOMIC_HIT_SCRIPT =
            "local current = redis.call('INCR', KEYS[1])\n" +
            "if current == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[1])\n" +
            "end\n" +
            "return current";

    private static final DefaultRedisScript<Long> HIT_SCRIPT = new DefaultRedisScript<>(ATOMIC_HIT_SCRIPT, Long.class);

    private final StringRedisTemplate redis;

    public ValkeyRateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean hit(String key, int limit, int ttlSeconds) {
        Long current = redis.execute(HIT_SCRIPT, List.of(key), String.valueOf(ttlSeconds));
        if (current == null) return false;
        return current <= limit;
    }

    public long current(String key) {
        String val = redis.opsForValue().get(key);
        return val != null ? Long.parseLong(val) : 0;
    }

    public void reset(String key) {
        redis.delete(key);
    }
}
