package com.omnixys.cache.core;

import java.util.Objects;

public class ValkeyKeyDefinition {

    private final String prefix;
    private final int ttlSeconds;

    public ValkeyKeyDefinition(String prefix) {
        this(prefix, -1);
    }

    public ValkeyKeyDefinition(String prefix, int ttlSeconds) {
        if (prefix == null || prefix.isBlank()) throw new IllegalArgumentException("prefix must not be blank");
        this.prefix = prefix;
        this.ttlSeconds = ttlSeconds;
    }

    public String prefix() { return prefix; }

    public int ttlSeconds() { return ttlSeconds; }

    public boolean hasTtl() { return ttlSeconds > 0; }

    public String key(String... parts) {
        return prefix + ":" + String.join(":", parts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValkeyKeyDefinition that)) return false;
        return prefix.equals(that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix);
    }
}
