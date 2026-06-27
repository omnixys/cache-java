package com.omnixys.cache.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class DelayedJobRegistry {

    private final Map<String, BiConsumer<String, Map<String, Object>>> handlers = new ConcurrentHashMap<>();

    public void register(String type, BiConsumer<String, Map<String, Object>> handler) {
        if (handlers.containsKey(type)) {
            throw new IllegalArgumentException("Duplicate handler for job type: " + type);
        }
        handlers.put(type, handler);
    }

    public void execute(String type, String jobId, Map<String, Object> payload) {
        BiConsumer<String, Map<String, Object>> handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for job type: " + type);
        }
        handler.accept(jobId, payload);
    }

    public boolean hasHandler(String type) {
        return handlers.containsKey(type);
    }

    public void clear() {
        handlers.clear();
    }
}
