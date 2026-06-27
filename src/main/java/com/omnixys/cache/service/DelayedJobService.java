package com.omnixys.cache.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.omnixys.cache.model.DelayedJobContext;
import com.omnixys.cache.model.DelayedJobSchedule;
import com.omnixys.cache.model.DelayedJobStatus;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class DelayedJobService {

    private static final String DEFAULT_STREAM = "delayed:jobs";
    private static final long RECORD_TTL_SECONDS = 7 * 24 * 60 * 60;

    private final StringRedisTemplate redis;
    private final ValkeyStreamService streamService;
    private final ObjectMapper mapper;
    private final String keyPrefix;

    public DelayedJobService(StringRedisTemplate redis, ValkeyStreamService streamService,
                             String keyPrefix) {
        this.redis = redis;
        this.streamService = streamService;
        this.mapper = new ObjectMapper();
        this.keyPrefix = keyPrefix != null ? keyPrefix + ":" : "";
    }

    public String schedule(DelayedJobSchedule input) {
        String jobId = UUID.randomUUID().toString();
        long executeAt = System.currentTimeMillis() + input.delayMs();

        DelayedJobStatus status = new DelayedJobStatus(
                jobId, input.type(), input.payload(), executeAt,
                0, Math.max(input.maxRetries(), 0),
                Math.max(input.retryDelayMs(), 1000),
                input.stream() != null ? input.stream() : DEFAULT_STREAM,
                captureContext(),
                "scheduled", Instant.now(), Instant.now(), null
        );

        persist(status);
        redis.opsForZSet().add(queueKey(), jobId, executeAt);
        return jobId;
    }

    public boolean cancel(String id) {
        DelayedJobStatus job = getStatus(id);
        if (job == null) return false;
        if (!job.isScheduled()) return false;

        redis.opsForZSet().remove(queueKey(), id);
        DelayedJobStatus canceled = new DelayedJobStatus(
                job.id(), job.type(), job.payload(), job.executeAtEpochMs(),
                job.retries(), job.maxRetries(), job.retryDelayMs(),
                job.stream(), job.context(),
                "canceled", job.createdAt(), Instant.now(), null
        );
        persist(canceled);
        return true;
    }

    public DelayedJobStatus getStatus(String id) {
        String raw = redis.opsForValue().get(recordKey(id));
        if (raw == null) return null;
        try {
            return mapper.readValue(raw, DelayedJobStatus.class);
        } catch (JacksonException e) {
            return null;
        }
    }

    public List<DelayedJobStatus> claimDue(int count) {
        long now = System.currentTimeMillis();
        Set<String> ids = redis.opsForZSet().rangeByScore(queueKey(), 0, now, 0, count);
        if (ids == null || ids.isEmpty()) return List.of();

        List<DelayedJobStatus> claimed = new ArrayList<>();
        for (String id : ids) {
            Long removed = redis.opsForZSet().remove(queueKey(), id);
            if (removed == null || removed == 0) continue;

            DelayedJobStatus job = getStatus(id);
            if (job == null || !job.isScheduled()) continue;

            DelayedJobStatus running = new DelayedJobStatus(
                    job.id(), job.type(), job.payload(), job.executeAtEpochMs(),
                    job.retries(), job.maxRetries(), job.retryDelayMs(),
                    job.stream(), job.context(),
                    "running", job.createdAt(), Instant.now(), null
            );
            persist(running);
            claimed.add(running);
        }
        return claimed;
    }

    public void complete(DelayedJobStatus job) {
        DelayedJobStatus completed = new DelayedJobStatus(
                job.id(), job.type(), job.payload(), job.executeAtEpochMs(),
                job.retries(), job.maxRetries(), job.retryDelayMs(),
                job.stream(), job.context(),
                "completed", job.createdAt(), Instant.now(), null
        );
        persist(completed);
    }

    public void fail(DelayedJobStatus job, String error) {
        int newRetries = job.retries() + 1;
        if (newRetries <= job.maxRetries()) {
            long backoffDelay = (long) job.retryDelayMs() * newRetries;
            long newExecuteAt = System.currentTimeMillis() + backoffDelay;
            DelayedJobStatus retrying = new DelayedJobStatus(
                    job.id(), job.type(), job.payload(), newExecuteAt,
                    newRetries, job.maxRetries(), job.retryDelayMs(),
                    job.stream(), job.context(),
                    "scheduled", job.createdAt(), Instant.now(), error
            );
            persist(retrying);
            redis.opsForZSet().add(queueKey(), job.id(), newExecuteAt);
        } else {
            DelayedJobStatus failed = new DelayedJobStatus(
                    job.id(), job.type(), job.payload(), job.executeAtEpochMs(),
                    newRetries, job.maxRetries(), job.retryDelayMs(),
                    job.stream(), job.context(),
                    "failed", job.createdAt(), Instant.now(), error
            );
            persist(failed);
        }
    }

    private void persist(DelayedJobStatus job) {
        try {
            String json = mapper.writeValueAsString(job);
            redis.opsForValue().set(recordKey(job.id()), json, Duration.ofSeconds(RECORD_TTL_SECONDS));
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize job: " + job.id(), e);
        }
    }

    private String queueKey() {
        return keyPrefix + "delayed:jobs:scheduled";
    }

    private String recordKey(String id) {
        return keyPrefix + "delayed:job:" + id;
    }

    private DelayedJobContext captureContext() {
        return new DelayedJobContext(null, null, null, null, null);
    }
}
