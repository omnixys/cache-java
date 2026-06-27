package com.omnixys.cache.model;

public record StreamMessage<T>(
        String id,
        T data
) {}
