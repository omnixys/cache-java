package com.omnixys.cache.exception;

import com.omnixys.commons.error.BaseOmnixysException;
import com.omnixys.commons.error.ErrorCode;

public class CacheValidationError extends BaseOmnixysException {
    private final String key;

    public CacheValidationError(String key, String message) {
        super(ErrorCode.CACHE_VALIDATION_ERROR, message);
        this.key = key;
    }

    public String getKey() { return key; }
}
