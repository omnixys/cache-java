package com.omnixys.cache.core;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class JsonCacheSerializer implements CacheSerializer {

    private final ObjectMapper mapper;

    public JsonCacheSerializer() {
        this.mapper = new JsonMapper();
    }

    public JsonCacheSerializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String serialize(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize cache value", e);
        }
    }

    @Override
    public <T> T deserialize(String value, Class<T> type) {
        if (value == null || value.isBlank()) return null;
        try {
            return mapper.readValue(value, type);
        } catch (JacksonException e) {
            return null;
        }
    }
}
