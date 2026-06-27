package com.omnixys.cache.service;

import com.omnixys.cache.model.DelayedJobStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DelayedJobWorker {

    private final DelayedJobService jobs;
    private final DelayedJobRegistry registry;
    private final long pollIntervalMs;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger inFlight = new AtomicInteger(0);
    private Thread workerThread;

    public DelayedJobWorker(DelayedJobService jobs, DelayedJobRegistry registry, long pollIntervalMs) {
        this.jobs = jobs;
        this.registry = registry;
        this.pollIntervalMs = pollIntervalMs > 0 ? pollIntervalMs : 1000;
    }

    @PostConstruct
    public void start() {
        if (running.compareAndSet(false, true)) {
            workerThread = Thread.ofVirtual().start(this::run);
        }
    }

    @PreDestroy
    public void close() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public int inFlight() {
        return inFlight.get();
    }

    private void run() {
        while (running.get()) {
            try {
                List<DelayedJobStatus> dueJobs = jobs.claimDue(10);
                for (DelayedJobStatus job : dueJobs) {
                    if (!running.get()) break;
                    inFlight.incrementAndGet();
                    Thread.ofVirtual().name("delayed-job-" + job.id()).start(() -> processJob(job));
                }
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processJob(DelayedJobStatus job) {
        try {
            registry.execute(job.type(), job.id(), job.payload());
            jobs.complete(job);
        } catch (Exception e) {
            jobs.fail(job, e.getMessage());
        } finally {
            inFlight.decrementAndGet();
        }
    }
}
