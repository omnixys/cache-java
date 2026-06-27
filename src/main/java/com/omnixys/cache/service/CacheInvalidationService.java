package com.omnixys.cache.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.omnixys.cache.model.CacheInvalidationEvent;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public class CacheInvalidationService {

    private final StringRedisTemplate redis;
    private final RedisMessageListenerContainer container;
    private final ObjectMapper mapper;
    private final String channel;
    private final String source;
    private final Set<Consumer<CacheInvalidationEvent>> handlers = new CopyOnWriteArraySet<>();

    public CacheInvalidationService(StringRedisTemplate redis, RedisMessageListenerContainer container,
                                    String channel, String source) {
        this.redis = redis;
        this.container = container;
        this.mapper = new ObjectMapper();
        this.channel = channel;
        this.source = source;
        subscribe();
    }

    private void subscribe() {
        MessageListener listener = (message, pattern) -> {
            try {
                CacheInvalidationEvent event = mapper.readValue(
                        new String(message.getBody()), CacheInvalidationEvent.class);
                for (Consumer<CacheInvalidationEvent> handler : handlers) {
                    handler.accept(event);
                }
            } catch (Exception e) {
                // Ignore malformed invalidation messages
            }
        };
        container.addMessageListener(listener, ChannelTopic.of(channel));
    }

    public void publish(String key, String operation) {
        CacheInvalidationEvent event = new CacheInvalidationEvent(
                key, operation, source, System.currentTimeMillis(), null, null);
        try {
            String json = mapper.writeValueAsString(event);
            redis.convertAndSend(channel, json);
        } catch (JacksonException ignored) {
        }
    }

    public Runnable onInvalidation(Consumer<CacheInvalidationEvent> handler) {
        handlers.add(handler);
        return () -> handlers.remove(handler);
    }

    public void close() {
        handlers.clear();
    }
}
