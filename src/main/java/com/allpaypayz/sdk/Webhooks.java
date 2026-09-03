package com.allpaypayz.sdk;

import com.allpaypayz.sdk.exception.AllpaypayzWebhookException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Webhook signature verification.
 * <p>
 * The signer in the core service writes a {@code Callback-Signature} header
 * of the form {@code t=<unix-seconds>,v1=<hex>}. {@link #verify} recomputes
 * {@code HMAC-SHA256(t + "." + raw_body, signKey)}, runs a constant-time
 * compare, rejects deliveries outside the tolerance window, then returns the
 * parsed {@code event} field.
 */
public final class Webhooks {
    private static final Pattern SIGNATURE = Pattern.compile("^t=(\\d+),v1=([0-9a-fA-F]+)$");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Webhooks() {}

    public static Map<String, Object> verify(
            byte[] rawBody,
            String signatureHeader,
            String signKey
    ) {
        return verify(rawBody, signatureHeader, signKey, 300, Clock.systemUTC());
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> verify(
            byte[] rawBody,
            String signatureHeader,
            String signKey,
            int toleranceSeconds,
            Clock clock
    ) {
        if (signatureHeader == null) {
            throw new AllpaypayzWebhookException("invalid_signature_header", "Callback-Signature header is missing");
        }
        Matcher m = SIGNATURE.matcher(signatureHeader.trim());
        if (!m.matches()) {
            throw new AllpaypayzWebhookException(
                    "invalid_signature_header",
                    "Malformed Callback-Signature: " + signatureHeader);
        }
        long ts = Long.parseLong(m.group(1));
        String v1Hex = m.group(2).toLowerCase();

        byte[] expected = hmacSha256Hex(signKey, ts, rawBody == null ? new byte[0] : rawBody);
        if (!MessageDigest.isEqual(expected, v1Hex.getBytes(StandardCharsets.US_ASCII))) {
            throw new AllpaypayzWebhookException("signature_mismatch", "Webhook signature does not match");
        }

        long now = Instant.now(clock).getEpochSecond();
        if (Math.abs(now - ts) > toleranceSeconds) {
            throw new AllpaypayzWebhookException(
                    "stale_delivery",
                    "Webhook timestamp " + ts + " outside " + toleranceSeconds + "s tolerance (now=" + now + ")");
        }

        if (rawBody == null || rawBody.length == 0) {
            throw new AllpaypayzWebhookException("invalid_envelope", "Webhook body is empty");
        }
        Map<String, Object> parsed;
        try {
            parsed = MAPPER.readValue(rawBody, Map.class);
        } catch (IOException e) {
            throw new AllpaypayzWebhookException("invalid_json", "Webhook body is not valid JSON: " + e.getMessage());
        }
        Object eventObj = parsed.get("event");
        if (!(eventObj instanceof Map)) {
            throw new AllpaypayzWebhookException("invalid_envelope", "Webhook envelope missing event field");
        }
        Map<String, Object> event = (Map<String, Object>) eventObj;
        if (!event.containsKey("type")) {
            throw new AllpaypayzWebhookException("invalid_envelope", "Webhook event missing type field");
        }
        return event;
    }

    private static byte[] hmacSha256Hex(String key, long ts, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((ts + ".").getBytes(StandardCharsets.UTF_8));
            mac.update(body);
            return toHex(mac.doFinal()).getBytes(StandardCharsets.US_ASCII);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new AllpaypayzWebhookException("crypto_unavailable", e.getMessage());
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
