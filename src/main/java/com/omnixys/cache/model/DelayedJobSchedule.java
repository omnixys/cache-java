package com.omnixys.cache.model;

import java.util.Map;

public record DelayedJobSchedule(
        String stream,
        String type,
        Map<String, Object> payload,
        long delayMs,
        int retries,
        int maxRetries,
        int retryDelayMs
) {
    public DelayedJobSchedule {
        if (type == null || type.isBlank()) throw new IllegalArgumentException("type must not be blank");
        if (payload == null) throw new IllegalArgumentException("payload must not be null");
        if (delayMs < 0) throw new IllegalArgumentException("delayMs must not be negative");
    }
}
