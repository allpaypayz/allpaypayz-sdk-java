package com.allpaypayz.sdk.http;

import com.allpaypayz.sdk.exception.AllpaypayzException;
import com.allpaypayz.sdk.exception.AuthenticationException;
import com.allpaypayz.sdk.exception.BusinessException;
import com.allpaypayz.sdk.exception.ConflictException;
import com.allpaypayz.sdk.exception.GatewayException;
import com.allpaypayz.sdk.exception.NetworkException;
import com.allpaypayz.sdk.exception.NotFoundException;
import com.allpaypayz.sdk.exception.RateLimitException;
import com.allpaypayz.sdk.exception.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Internal HTTP layer used by every resource. Wraps {@link java.net.http.HttpClient}
 * with auth, auto-idempotency, retries with jitter, and v4-shaped error mapping.
 */
public final class HttpClient {
    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(429, 500, 502, 503, 504);

    private final java.net.http.HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String userAgent;
    private final String apiVersion;
    private final RetryOptions retry;
    private final Duration requestTimeout;
    private final ObjectMapper mapper;
    private final Random random;

    public HttpClient(
            String apiKey,
            String baseUrl,
            String userAgent,
            String apiVersion,
            RetryOptions retry,
            Duration requestTimeout,
            java.net.http.HttpClient httpClient
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.userAgent = userAgent;
        this.apiVersion = apiVersion;
        this.retry = retry;
        this.requestTimeout = requestTimeout;
        this.httpClient = httpClient != null
                ? httpClient
                : java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
        this.mapper = new ObjectMapper();
        this.random = ThreadLocalRandom.current();
    }

    public Map<String, Object> request(
            String method,
            String path,
            Map<String, Object> body,
            Map<String, String> query,
            String idempotencyKey
    ) {
        URI uri = buildUri(path, query);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json");
        if (apiVersion != null) {
            builder = builder.header("Accept-Api-Version", apiVersion);
        }

        HttpRequest.BodyPublisher publisher;
        if (body != null) {
            try {
                publisher = HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(body));
            } catch (IOException e) {
                throw new AllpaypayzException("api", "request_serialize_failed", e.getMessage(),
                        null, null, null, null);
            }
            builder = builder.header("Content-Type", "application/json");
        } else {
            publisher = HttpRequest.BodyPublishers.noBody();
        }

        if ("POST".equalsIgnoreCase(method)) {
            String key = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
            builder = builder.header("Idempotency-Key", key);
        }

        builder = builder.method(method.toUpperCase(), publisher);
        HttpRequest httpRequest = builder.build();

        Throwable lastError = null;
        for (int attempt = 1; attempt <= retry.maxAttempts; attempt++) {
            HttpResponse<byte[]> response;
            try {
                response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new NetworkException("network", "interrupted", ie.getMessage(),
                        null, null, null, null);
            } catch (IOException e) {
                if (attempt < retry.maxAttempts) {
                    sleep(attempt, null);
                    lastError = e;
                    continue;
                }
                throw new NetworkException("network", "network_error", e.getMessage(),
                        null, null, null, null);
            }

            int status = response.statusCode();
            if (status < 400) {
                return parseJson(response.body());
            }

            Integer retryAfter = parseRetryAfter(response.headers().firstValue("Retry-After").orElse(null));
            Map<String, Object> payload = safeJson(response.body());
            AllpaypayzException err = buildException(status, payload, retryAfter);
            if (RETRYABLE_STATUSES.contains(status) && attempt < retry.maxAttempts) {
                sleep(attempt, retryAfter);
                lastError = err;
                continue;
            }
            throw err;
        }
        if (lastError instanceof RuntimeException re) throw re;
        throw new AllpaypayzException("api", "retry_exhausted", "all retries failed",
                null, null, null, null);
    }

    private URI buildUri(String path, Map<String, String> query) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String p = path.startsWith("/") ? path : "/" + path;
        StringBuilder sb = new StringBuilder(base).append(p);
        if (query != null) {
            String qs = encodeQuery(query);
            if (!qs.isEmpty()) sb.append('?').append(qs);
        }
        return URI.create(sb.toString());
    }

    private static String encodeQuery(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null) continue;
            if (!first) sb.append('&');
            first = false;
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(byte[] body) {
        if (body == null || body.length == 0) return new LinkedHashMap<>();
        try {
            return mapper.readValue(body, Map.class);
        } catch (IOException e) {
            throw new AllpaypayzException("api", "invalid_json_response", e.getMessage(),
                    null, null, null, null);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeJson(byte[] body) {
        if (body == null || body.length == 0) return null;
        try {
            return mapper.readValue(body, Map.class);
        } catch (IOException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private AllpaypayzException buildException(int status, Map<String, Object> payload, Integer retryAfter) {
        Map<String, Object> err = payload != null
                ? (Map<String, Object>) payload.getOrDefault("error", Map.of())
                : Map.of();
        String type = (String) err.getOrDefault("type", statusToType(status));
        String code = (String) err.getOrDefault("code", "http_" + status);
        String message = (String) err.getOrDefault("message", "Request failed with status " + status);
        Object details = err.get("details");
        String requestId = payload != null ? (String) payload.get("request_id") : null;
        List<Map<String, Object>> typedDetails = details instanceof List ? (List<Map<String, Object>>) details : null;

        switch (type) {
            case "validation": return new ValidationException(type, code, message, status, requestId, typedDetails, retryAfter);
            case "authentication": return new AuthenticationException(type, code, message, status, requestId, typedDetails, retryAfter);
            case "not_found": return new NotFoundException(type, code, message, status, requestId, typedDetails, retryAfter);
            case "conflict": return new ConflictException(type, code, message, status, requestId, typedDetails, retryAfter);
            case "business": return new BusinessException(type, code, message, status, requestId, typedDetails, retryAfter);
            case "rate_limit": return new RateLimitException(type, code, message, status, requestId, typedDetails, retryAfter);
            case "gateway": return new GatewayException(type, code, message, status, requestId, typedDetails, retryAfter);
            default: return new AllpaypayzException(type, code, message, status, requestId, typedDetails, retryAfter);
        }
    }

    private static String statusToType(int status) {
        if (status == 400) return "validation";
        if (status == 401 || status == 403) return "authentication";
        if (status == 404) return "not_found";
        if (status == 409) return "conflict";
        if (status == 422) return "business";
        if (status == 429) return "rate_limit";
        if (status >= 500 && status <= 599) return "gateway";
        return "api";
    }

    private static Integer parseRetryAfter(String header) {
        if (header == null || header.isBlank()) return null;
        try {
            return Integer.parseInt(header.trim());
        } catch (NumberFormatException ignored) {
            // ignore HTTP-date form for now
            return null;
        }
    }

    private void sleep(int attempt, Integer retryAfter) {
        try {
            if (retryAfter != null) {
                Thread.sleep(retryAfter * 1000L);
                return;
            }
            long exp = Math.min(retry.maxBackoff.toMillis(),
                    retry.initialBackoff.toMillis() * (1L << (attempt - 1)));
            long delay = exp + random.nextInt((int) Math.max(1, retry.jitter.toMillis() + 1));
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
