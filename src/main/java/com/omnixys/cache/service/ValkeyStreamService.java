package com.omnixys.cache.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.omnixys.cache.model.StreamMessage;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.*;

public class ValkeyStreamService {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public ValkeyStreamService(StringRedisTemplate redis) {
        this(redis, new ObjectMapper());
    }

    public ValkeyStreamService(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    public void ensureGroup(String stream, String group) {
        try {
            redis.opsForStream().createGroup(stream, group);
        } catch (Exception e) {
            // BUSYGROUP error — group already exists, that's fine
        }
    }

    public String enqueue(String stream, Object payload) {
        try {
            String json = mapper.writeValueAsString(payload);
            Map<String, String> body = new HashMap<>();
            body.put("data", json);
            RecordId id = redis.opsForStream().add(stream, body);
            return id != null ? id.getValue() : null;
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize stream payload", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<StreamMessage<T>> consume(String stream, String group, String consumer,
                                               Class<T> type, int count, long blockMs) {
        StreamReadOptions options = StreamReadOptions.empty()
                .count(count)
                .block(Duration.ofMillis(blockMs));

        List<MapRecord<String, Object, Object>> records =
                (List<MapRecord<String, Object, Object>>) (List<?>)
                        redis.opsForStream().read(
                                Consumer.from(group, consumer),
                                options,
                                StreamOffset.create(stream, ReadOffset.lastConsumed()));

        if (records == null) return List.of();

        List<StreamMessage<T>> messages = new ArrayList<>();
        for (MapRecord<String, Object, Object> record : records) {
            try {
                String data = (String) record.getValue().get("data");
                T payload = mapper.readValue(data, type);
                messages.add(new StreamMessage<>(record.getId().getValue(), payload));
            } catch (Exception e) {
                // Skip malformed messages
            }
        }
        return messages;
    }

    public void ack(String stream, String group, String id) {
        redis.opsForStream().acknowledge(stream, group, id);
    }

    @SuppressWarnings("unchecked")
    public List<StreamMessage<String>> readPending(String stream, String group,
                                                    String consumer, int count) {
        StreamReadOptions options = StreamReadOptions.empty().count(count);

        List<MapRecord<String, Object, Object>> records =
                (List<MapRecord<String, Object, Object>>) (List<?>)
                        redis.opsForStream().read(
                                Consumer.from(group, consumer),
                                options,
                                StreamOffset.create(stream, ReadOffset.from("0")));

        if (records == null) return List.of();

        List<StreamMessage<String>> messages = new ArrayList<>();
        for (MapRecord<String, Object, Object> record : records) {
            String data = (String) record.getValue().get("data");
            messages.add(new StreamMessage<>(record.getId().getValue(), data));
        }
        return messages;
    }
}
