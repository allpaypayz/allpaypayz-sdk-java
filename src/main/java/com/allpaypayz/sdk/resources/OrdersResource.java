package com.allpaypayz.sdk.resources;

import com.allpaypayz.sdk.http.HttpClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public final class OrdersResource {
    private final HttpClient http;

    public OrdersResource(HttpClient http) {
        this.http = http;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> data(Map<String, Object> env) {
        return (Map<String, Object>) env.get("data");
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public Map<String, Object> create(Map<String, Object> body, String idempotencyKey) {
        return data(http.request("POST", "/v4/orders", body, null, idempotencyKey));
    }

    public Map<String, Object> create(Map<String, Object> body) {
        return create(body, null);
    }

    public Map<String, Object> get(String id) {
        return data(http.request("GET", "/v4/orders/" + enc(id), null, null, null));
    }

    public Map<String, Object> findByReference(String merchantReference) {
        return data(http.request("GET", "/v4/orders", null,
                Collections.singletonMap("merchant_reference", merchantReference), null));
    }
}
