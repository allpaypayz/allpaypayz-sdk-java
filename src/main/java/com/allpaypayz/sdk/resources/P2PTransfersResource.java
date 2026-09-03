package com.allpaypayz.sdk.resources;

import com.allpaypayz.sdk.http.HttpClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public final class P2PTransfersResource {
    private final HttpClient http;

    public P2PTransfersResource(HttpClient http) {
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
        return data(http.request("POST", "/v4/p2p-transfers", body, null, idempotencyKey));
    }

    public Map<String, Object> create(Map<String, Object> body) {
        return create(body, null);
    }

    public Map<String, Object> confirm(String id, Map<String, Object> body, String idempotencyKey) {
        return data(http.request("POST", "/v4/p2p-transfers/" + enc(id) + "/confirm",
                body, null, idempotencyKey));
    }

    public Map<String, Object> confirm(String id, Map<String, Object> body) {
        return confirm(id, body, null);
    }

    public Map<String, Object> get(String id) {
        return data(http.request("GET", "/v4/p2p-transfers/" + enc(id), null, null, null));
    }

    public Map<String, Object> findByReference(String merchantReference) {
        return data(http.request("GET", "/v4/p2p-transfers", null,
                Collections.singletonMap("merchant_reference", merchantReference), null));
    }
}
