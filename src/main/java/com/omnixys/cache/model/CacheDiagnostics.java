package com.omnixys.cache.model;

public record CacheDiagnostics(
        String status,
        int activeOperations,
        boolean closing
) {}
