package com.allpaypayz.sdk.resources;

import com.allpaypayz.sdk.http.HttpClient;

import java.util.Map;

public final class TerminalResource {
    private final HttpClient http;

    public TerminalResource(HttpClient http) {
        this.http = http;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> get() {
        Map<String, Object> env = http.request("GET", "/v4/terminal", null, null, null);
        return (Map<String, Object>) env.get("data");
    }
}
