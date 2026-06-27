package com.omnixys.cache.core;

public interface CacheSerializer {
    String serialize(Object value);
    <T> T deserialize(String value, Class<T> type);
}
