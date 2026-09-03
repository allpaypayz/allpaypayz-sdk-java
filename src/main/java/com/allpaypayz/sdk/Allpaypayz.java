package com.allpaypayz.sdk;

import com.allpaypayz.sdk.http.HttpClient;
import com.allpaypayz.sdk.http.RetryOptions;
import com.allpaypayz.sdk.resources.OrdersResource;
import com.allpaypayz.sdk.resources.P2PTransfersResource;
import com.allpaypayz.sdk.resources.PaymentsResource;
import com.allpaypayz.sdk.resources.PayoutsResource;
import com.allpaypayz.sdk.resources.TerminalResource;

import java.time.Duration;

/**
 * Top-level Allpaypayz API v4 client.
 *
 * <pre>{@code
 * Allpaypayz client = Allpaypayz.builder().apiKey("sk_test_...").build();
 * Map<String, Object> payment = client.payments().create(Map.of(
 *     "merchant_reference", "ORDER-77",
 *     "amount", Map.of("amount_minor", 1000, "currency", "USD"),
 *     "card", Map.of("pan", "4111", "exp_month", 12, "exp_year", 2029, "cvc", "123")
 * ));
 * }</pre>
 */
public final class Allpaypayz {
    public static final String VERSION = "0.1.0";
    private static final String DEFAULT_BASE_URL = "https://api4.allpaypayz.com";
    private static final String BASE_USER_AGENT = "Allpaypayz-SDK-Java/" + VERSION;

    private final HttpClient http;
    private final PaymentsResource payments;
    private final PayoutsResource payouts;
    private final P2PTransfersResource p2p;
    private final OrdersResource orders;
    private final TerminalResource terminal;

    private Allpaypayz(Builder b) {
        if (b.apiKey == null || b.apiKey.isEmpty()) {
            throw new IllegalArgumentException("Allpaypayz: apiKey is required");
        }
        String userAgent = b.userAgent != null
                ? BASE_USER_AGENT + " " + b.userAgent
                : BASE_USER_AGENT;
        this.http = new HttpClient(
                b.apiKey,
                b.baseUrl != null ? b.baseUrl : DEFAULT_BASE_URL,
                userAgent,
                b.apiVersion,
                b.retry != null ? b.retry : RetryOptions.defaults(),
                b.requestTimeout != null ? b.requestTimeout : Duration.ofSeconds(30),
                b.httpClient
        );
        this.payments = new PaymentsResource(http);
        this.payouts = new PayoutsResource(http);
        this.p2p = new P2PTransfersResource(http);
        this.orders = new OrdersResource(http);
        this.terminal = new TerminalResource(http);
    }

    public PaymentsResource payments() { return payments; }
    public PayoutsResource payouts() { return payouts; }
    public P2PTransfersResource p2p() { return p2p; }
    public OrdersResource orders() { return orders; }
    public TerminalResource terminal() { return terminal; }

    /** Exposed for tests / power users; do not use for normal requests. */
    public HttpClient http() { return http; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String apiKey;
        private String baseUrl;
        private String apiVersion;
        private String userAgent;
        private RetryOptions retry;
        private Duration requestTimeout;
        private java.net.http.HttpClient httpClient;

        public Builder apiKey(String v) { this.apiKey = v; return this; }
        public Builder baseUrl(String v) { this.baseUrl = v; return this; }
        public Builder apiVersion(String v) { this.apiVersion = v; return this; }
        public Builder userAgent(String v) { this.userAgent = v; return this; }
        public Builder retry(RetryOptions v) { this.retry = v; return this; }
        public Builder requestTimeout(Duration v) { this.requestTimeout = v; return this; }
        public Builder httpClient(java.net.http.HttpClient v) { this.httpClient = v; return this; }

        public Allpaypayz build() {
            return new Allpaypayz(this);
        }
    }
}
