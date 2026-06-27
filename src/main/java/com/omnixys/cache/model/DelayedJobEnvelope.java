package com.omnixys.cache.model;

import java.util.Map;

public record DelayedJobEnvelope(
        String id,
        String type,
        Map<String, Object> payload,
        long executeAtEpochMs,
        int retries,
        int maxRetries,
        int retryDelayMs,
        String stream,
        DelayedJobContext context
) {}
