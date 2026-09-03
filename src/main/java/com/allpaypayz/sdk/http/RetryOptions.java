package com.allpaypayz.sdk.http;

import java.time.Duration;

public final class RetryOptions {
    public final int maxAttempts;
    public final Duration initialBackoff;
    public final Duration maxBackoff;
    public final Duration jitter;

    public RetryOptions(int maxAttempts, Duration initialBackoff, Duration maxBackoff, Duration jitter) {
        this.maxAttempts = maxAttempts;
        this.initialBackoff = initialBackoff;
        this.maxBackoff = maxBackoff;
        this.jitter = jitter;
    }

    public static RetryOptions defaults() {
        return new RetryOptions(3, Duration.ofMillis(250), Duration.ofSeconds(4), Duration.ofMillis(250));
    }
}
