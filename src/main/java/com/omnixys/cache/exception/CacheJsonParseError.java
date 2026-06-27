package com.omnixys.cache.exception;

import com.omnixys.commons.error.BaseOmnixysException;
import com.omnixys.commons.error.ErrorCode;

public class CacheJsonParseError extends BaseOmnixysException {
    public CacheJsonParseError(String message, Throwable cause) {
        super(ErrorCode.CACHE_PARSE_ERROR, message, cause);
    }
}
