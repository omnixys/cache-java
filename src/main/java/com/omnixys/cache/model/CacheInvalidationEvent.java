package com.omnixys.cache.model;

public record CacheInvalidationEvent(
        String key,
        String operation,
        String source,
        long occurredAtEpochMs,
        String requestId,
        String correlationId
) {}
