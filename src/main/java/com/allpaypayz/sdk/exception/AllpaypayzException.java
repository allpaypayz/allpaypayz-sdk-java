package com.allpaypayz.sdk.exception;

import java.util.List;
import java.util.Map;

public class AllpaypayzException extends RuntimeException {
    private final String type;
    private final String errorCode;
    private final Integer status;
    private final String requestId;
    private final List<Map<String, Object>> details;
    private final Integer retryAfterSeconds;

    public AllpaypayzException(
            String type,
            String errorCode,
            String message,
            Integer status,
            String requestId,
            List<Map<String, Object>> details,
            Integer retryAfterSeconds
    ) {
        super(message);
        this.type = type;
        this.errorCode = errorCode;
        this.status = status;
        this.requestId = requestId;
        this.details = details;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getType() { return type; }
    public String getErrorCode() { return errorCode; }
    public Integer getStatus() { return status; }
    public String getRequestId() { return requestId; }
    public List<Map<String, Object>> getDetails() { return details; }
    public Integer getRetryAfterSeconds() { return retryAfterSeconds; }
}
