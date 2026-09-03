package com.allpaypayz.sdk.resources;

import com.allpaypayz.sdk.http.HttpClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public final class PaymentsResource {
    private final HttpClient http;

    public PaymentsResource(HttpClient http) {
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
        return data(http.request("POST", "/v4/payments", body, null, idempotencyKey));
    }

    public Map<String, Object> create(Map<String, Object> body) {
        return create(body, null);
    }

    public Map<String, Object> createRedirect(Map<String, Object> body, String idempotencyKey) {
        return data(http.request("POST", "/v4/payments/redirect", body, null, idempotencyKey));
    }

    public Map<String, Object> createRedirect(Map<String, Object> body) {
        return createRedirect(body, null);
    }

    public Map<String, Object> recurrent(Map<String, Object> body, String idempotencyKey) {
        return data(http.request("POST", "/v4/payments/recurrent", body, null, idempotencyKey));
    }

    public Map<String, Object> recurrent(Map<String, Object> body) {
        return recurrent(body, null);
    }

    public Map<String, Object> finish3ds(String id, Map<String, Object> body, String idempotencyKey) {
        return data(http.request("POST", "/v4/payments/" + enc(id) + "/finish-3ds", body, null, idempotencyKey));
    }

    public Map<String, Object> finish3ds(String id, Map<String, Object> body) {
        return finish3ds(id, body, null);
    }

    public Map<String, Object> get(String id) {
        return data(http.request("GET", "/v4/payments/" + enc(id), null, null, null));
    }

    public Map<String, Object> findByReference(String merchantReference) {
        return data(http.request("GET", "/v4/payments", null,
                Collections.singletonMap("merchant_reference", merchantReference), null));
    }

    public Map<String, Object> createRefund(String paymentId, Map<String, Object> body, String idempotencyKey) {
        return data(http.request("POST",
                "/v4/payments/" + enc(paymentId) + "/refunds",
                body, null, idempotencyKey));
    }

    public Map<String, Object> createRefund(String paymentId, Map<String, Object> body) {
        return createRefund(paymentId, body, null);
    }

    public Map<String, Object> getRefund(String paymentId, String refundId) {
        return data(http.request("GET",
                "/v4/payments/" + enc(paymentId) + "/refunds/" + enc(refundId),
                null, null, null));
    }
}
