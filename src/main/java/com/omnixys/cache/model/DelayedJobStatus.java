package com.omnixys.cache.model;

import java.time.Instant;
import java.util.Map;

public record DelayedJobStatus(
        String id,
        String type,
        Map<String, Object> payload,
        long executeAtEpochMs,
        int retries,
        int maxRetries,
        int retryDelayMs,
        String stream,
        DelayedJobContext context,
        String status,
        Instant createdAt,
        Instant updatedAt,
        String lastError
) {
    public boolean isScheduled() { return "scheduled".equals(status); }
    public boolean isRunning() { return "running".equals(status); }
    public boolean isCompleted() { return "completed".equals(status); }
    public boolean isFailed() { return "failed".equals(status); }
    public boolean isCanceled() { return "canceled".equals(status); }
}
