package com.omnixys.cache.service;

import com.omnixys.cache.core.CacheSerializer;
import com.omnixys.cache.core.ValkeyKeyDefinition;
import com.omnixys.cache.model.CacheDiagnostics;
import com.omnixys.cache.model.CacheHealth;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ValkeyService {

    private final StringRedisTemplate redis;
    private final CacheSerializer serializer;
    private final CacheInvalidationService invalidation;
    private final String keyPrefix;

    public ValkeyService(StringRedisTemplate redis, CacheSerializer serializer,
                         CacheInvalidationService invalidation, String keyPrefix) {
        this.redis = redis;
        this.serializer = serializer;
        this.invalidation = invalidation;
        this.keyPrefix = keyPrefix != null ? keyPrefix + ":" : "";
    }

    public String prefixed(String key) {
        return keyPrefix + key;
    }

    public Optional<String> get(String key) {
        String val = redis.opsForValue().get(prefixed(key));
        return Optional.ofNullable(val);
    }

    public <T> Optional<T> getValue(String key, Class<T> type) {
        String raw = redis.opsForValue().get(prefixed(key));
        if (raw == null) return Optional.empty();
        return Optional.ofNullable(serializer.deserialize(raw, type));
    }

    public String set(String key, String value) {
        redis.opsForValue().set(prefixed(key), value);
        invalidation.publish(key, "set");
        return key;
    }

    public String set(String key, String value, int ttlSeconds) {
        redis.opsForValue().set(prefixed(key), value, Duration.ofSeconds(ttlSeconds));
        invalidation.publish(key, "set");
        return key;
    }

    public <T> String setValue(String key, T value) {
        redis.opsForValue().set(prefixed(key), serializer.serialize(value));
        invalidation.publish(key, "set");
        return key;
    }

    public <T> String setValue(String key, T value, int ttlSeconds) {
        redis.opsForValue().set(prefixed(key), serializer.serialize(value), Duration.ofSeconds(ttlSeconds));
        invalidation.publish(key, "set");
        return key;
    }

    public boolean delete(String key) {
        Boolean result = redis.delete(prefixed(key));
        invalidation.publish(key, "delete");
        return Boolean.TRUE.equals(result);
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(prefixed(key)));
    }

    public boolean expire(String key, int ttlSeconds) {
        return Boolean.TRUE.equals(redis.expire(prefixed(key), ttlSeconds, TimeUnit.SECONDS));
    }

    public long ttl(String key) {
        Long ttl = redis.getExpire(prefixed(key));
        return ttl != null ? ttl : -1;
    }

    public long increment(String key) {
        Long val = redis.opsForValue().increment(prefixed(key));
        return val != null ? val : 0;
    }

    public boolean setIfAbsent(String key, String value, int ttlSeconds) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(prefixed(key), value, Duration.ofSeconds(ttlSeconds)));
    }

    public CacheHealth health() {
        try {
            long start = System.currentTimeMillis();
            redis.getConnectionFactory().getConnection().ping();
            long latency = System.currentTimeMillis() - start;
            return new CacheHealth(true, "ready", latency, null);
        } catch (Exception e) {
            return new CacheHealth(false, "unavailable", null, e.getMessage());
        }
    }

    public CacheDiagnostics diagnostics() {
        return new CacheDiagnostics(null, 0, false);
    }
}
