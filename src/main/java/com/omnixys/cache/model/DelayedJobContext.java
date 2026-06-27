package com.omnixys.cache.model;

public record DelayedJobContext(
        String requestId,
        String correlationId,
        String traceId,
        String actorId,
        String tenantId
) {}
