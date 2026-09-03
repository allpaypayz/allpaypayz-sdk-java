# `com.allpaypayz:sdk` (Java)

**[⬇ Download the latest version](https://github.com/allpaypayz/allpaypayz-sdk-java/archive/refs/heads/main.zip)** · [Browse the code](https://github.com/allpaypayz/allpaypayz-sdk-java) · [MIT](LICENSE)

<sub>The archive is a snapshot of `main` — the current state of the SDK. Tagged releases will appear on the Releases page once the code leaves alpha.</sub>


Official Allpaypayz API v4 SDK for Java.

> Status: **alpha** (v0.1.0). Targets Java 17+ (current LTS).

## Install

### Maven

```xml
<dependency>
  <groupId>com.allpaypayz</groupId>
  <artifactId>sdk</artifactId>
  <version>0.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.allpaypayz:sdk:0.1.0'
```

The SDK uses the JDK-native `java.net.http.HttpClient` plus Jackson for JSON.
No third-party HTTP client dependency.

## Quick start

```java
import com.allpaypayz.sdk.Allpaypayz;

Allpaypayz client = Allpaypayz.builder()
    .apiKey(System.getenv("ALLPAYPAYZ_API_KEY"))
    .build();

Map<String, Object> payment = client.payments().create(Map.of(
    "merchant_reference", "ORDER-77",
    "amount",             Map.of("amount_minor", 1000, "currency", "USD"),
    "description",        "Order #77",
    "customer",           Map.of("name", "Jane Doe", "email", "jane@example.com"),
    "card", Map.of(
        "pan",       "4111111111111111",
        "exp_month", 12, "exp_year", 2029,
        "cvc",       "123", "holder", "JANE DOE"
    )
));

if ("requires_action".equals(payment.get("status"))) {
    // Redirect the customer through ((Map<String, Object>) payment.get("three_ds")).get("acs_url")
}
```

The SDK auto-injects `Idempotency-Key` (random UUIDv4) on every POST. Use the
two-arg overload `create(body, idempotencyKey)` to pass your own.

## Configuration

```java
import com.allpaypayz.sdk.http.RetryOptions;
import java.time.Duration;

Allpaypayz client = Allpaypayz.builder()
    .apiKey("...")
    .baseUrl("https://staging-api4.allpaypayz.com")
    .apiVersion("2026-05-20")
    .requestTimeout(Duration.ofSeconds(30))
    .retry(new RetryOptions(
        3,
        Duration.ofMillis(250),
        Duration.ofSeconds(4),
        Duration.ofMillis(250)
    ))
    .userAgent("MyApp/2.0")
    .build();
```

Inject your own `java.net.http.HttpClient` via `.httpClient(myClient)` to add
proxy / TLS / connection-pool settings.

## Resources

| Resource | Methods |
|---|---|
| `client.payments()` | `create`, `createRedirect`, `recurrent`, `finish3ds`, `get`, `findByReference`, `createRefund`, `getRefund` |
| `client.payouts()`  | `create`, `get`, `findByReference` |
| `client.p2p()`      | `create`, `confirm`, `get`, `findByReference` |
| `client.orders()`   | `create`, `get`, `findByReference` |
| `client.terminal()` | `get` |

`findByReference` looks up a single resource by `merchant_reference`.

## Errors

```java
import com.allpaypayz.sdk.exception.ConflictException;

try {
    client.payments().create(body);
} catch (ConflictException e) {
    if ("duplicate_reference".equals(e.getErrorCode())) {
        // merchant_reference already used on this terminal
    }
    throw e;
}
```

| HTTP / `error.type` | Class |
|---|---|
| `400` / `validation` | `ValidationException` |
| `401`, `403` / `authentication` | `AuthenticationException` |
| `404` / `not_found` | `NotFoundException` |
| `409` / `conflict` | `ConflictException` |
| `422` / `business` | `BusinessException` |
| `429` / `rate_limit` | `RateLimitException` (carries `getRetryAfterSeconds()`) |
| `5xx` / `gateway` | `GatewayException` |
| Network / transport | `NetworkException` |

All in `com.allpaypayz.sdk.exception`. Each carries `getType`, `getErrorCode`,
`getStatus`, `getRequestId`, `getDetails`, `getRetryAfterSeconds`.

## Webhooks

```java
import com.allpaypayz.sdk.Webhooks;
import com.allpaypayz.sdk.WebhookDispatcher;
import com.allpaypayz.sdk.exception.AllpaypayzWebhookException;

WebhookDispatcher dispatcher = new WebhookDispatcher()
    .on("payment.succeeded", event -> markOrderPaid((String) ((Map<?, ?>) event.get("resource")).get("merchant_reference")))
    .on("payment.failed",    event -> markOrderFailed((String) ((Map<?, ?>) event.get("resource")).get("merchant_reference")));

// In your servlet / controller:
byte[] body = request.getInputStream().readAllBytes();
String header = request.getHeader("Callback-Signature");
try {
    Map<String, Object> event = Webhooks.verify(body, header, System.getenv("ALLPAYPAYZ_SIGN_KEY"));
    dispatcher.dispatch(event);
    response.setStatus(200);
} catch (AllpaypayzWebhookException e) {
    response.setStatus(400);
    response.getWriter().write(e.getCode());
}
```

`Webhooks.verify` parses `Callback-Signature` (`t=<unix>,v1=<hex>`),
recomputes `HMAC-SHA256(t + "." + raw_body, signKey)` via `javax.crypto.Mac`
and `MessageDigest.isEqual` (constant-time), rejects deliveries outside the
300 s tolerance window (overridable via the 5-arg overload that also takes
a `Clock` for testing).

## Tests

```bash
mvn test
```

`WebhooksTest` runs against `../spec/test-vectors.json` to guarantee
byte-identity with every other Allpaypayz SDK. `AllpaypayzTest` spins up the
JDK-built-in `com.sun.net.httpserver.HttpServer` as a real backend; no
external mocking framework needed.

## License

MIT
