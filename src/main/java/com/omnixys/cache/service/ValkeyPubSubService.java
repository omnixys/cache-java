package com.omnixys.cache.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ValkeyPubSubService {

    private final StringRedisTemplate redis;
    private final RedisMessageListenerContainer container;
    private final ObjectMapper mapper;
    private final Set<String> channels = ConcurrentHashMap.newKeySet();
    private volatile boolean closing = false;

    public ValkeyPubSubService(StringRedisTemplate redis, RedisMessageListenerContainer container) {
        this(redis, container, new ObjectMapper());
    }

    public ValkeyPubSubService(StringRedisTemplate redis, RedisMessageListenerContainer container,
                               ObjectMapper mapper) {
        this.redis = redis;
        this.container = container;
        this.mapper = mapper;
    }

    public void publish(String channel, Object payload) {
        try {
            String json = mapper.writeValueAsString(payload);
            redis.convertAndSend(channel, json);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize pub/sub payload", e);
        }
    }

    public void subscribe(String channel, Consumer<String> handler) {
        channels.add(channel);
        MessageListener listener = (message, pattern) -> {
            String body = new String(message.getBody());
            handler.accept(body);
        };
        container.addMessageListener(listener, ChannelTopic.of(channel));
    }

    public void unsubscribe(String channel) {
        channels.remove(channel);
        container.removeMessageListener(null, ChannelTopic.of(channel));
    }

    public Set<String> activeChannels() {
        return Set.copyOf(channels);
    }

    public void close() {
        closing = true;
        for (String channel : channels) {
            container.removeMessageListener(null, ChannelTopic.of(channel));
        }
        channels.clear();
    }
}
