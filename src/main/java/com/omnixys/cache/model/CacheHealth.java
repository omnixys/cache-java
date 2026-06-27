package com.omnixys.cache.model;

public record CacheHealth(
        boolean healthy,
        String status,
        Long latencyMs,
        String error
) {}
