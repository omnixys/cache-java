package com.omnixys.cache.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "omnixys.cache")
public class ValkeyProperties {

    private String keyPrefix = "";
    private boolean workerEnabled = false;
    private long workerPollIntervalMs = 1000;
    private Invalidation invalidation = new Invalidation();

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public boolean isWorkerEnabled() { return workerEnabled; }
    public void setWorkerEnabled(boolean workerEnabled) { this.workerEnabled = workerEnabled; }

    public long getWorkerPollIntervalMs() { return workerPollIntervalMs; }
    public void setWorkerPollIntervalMs(long workerPollIntervalMs) { this.workerPollIntervalMs = workerPollIntervalMs; }

    public Invalidation getInvalidation() { return invalidation; }
    public void setInvalidation(Invalidation invalidation) { this.invalidation = invalidation; }

    public static class Invalidation {
        private boolean enabled = true;
        private String channel = "omnixys:cache:invalidate";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
    }
}
