package com.allpaypayz.sdk.exception;

public class AllpaypayzWebhookException extends RuntimeException {
    private final String code;

    public AllpaypayzWebhookException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
