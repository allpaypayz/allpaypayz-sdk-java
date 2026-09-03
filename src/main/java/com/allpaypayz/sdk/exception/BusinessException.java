package com.allpaypayz.sdk.exception;

import java.util.List;
import java.util.Map;

public class BusinessException extends AllpaypayzException {
    public BusinessException(
            String type,
            String errorCode,
            String message,
            Integer status,
            String requestId,
            List<Map<String, Object>> details,
            Integer retryAfterSeconds
    ) {
        super(type, errorCode, message, status, requestId, details, retryAfterSeconds);
    }
}
