package com.allpaypayz.sdk;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Routes a verified webhook event to a handler registered for its type. */
public final class WebhookDispatcher {
    private final Map<String, Consumer<Map<String, Object>>> handlers = new HashMap<>();

    public WebhookDispatcher on(String eventType, Consumer<Map<String, Object>> handler) {
        handlers.put(eventType, handler);
        return this;
    }

    public void dispatch(Map<String, Object> event) {
        Object type = event.get("type");
        if (!(type instanceof String)) return;
        Consumer<Map<String, Object>> h = handlers.get(type);
        if (h != null) {
            h.accept(event);
        }
    }
}
