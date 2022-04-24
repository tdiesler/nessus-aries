package io.nessus.aries.common.websocket;

import java.util.Objects;
import java.util.function.Consumer;

import io.nessus.aries.common.websocket.WebSocketEventHandler.WebSocketEvent;

public class FilteringEventSubscriber<T> extends EventSubscriber<WebSocketEvent> {

    private final String walletId;
    private final Class<T> eventType;
    private final Consumer<WebSocketEvent> consumer;
    
    public FilteringEventSubscriber(String walletId, Class<T> eventType, Consumer<WebSocketEvent> consumer) {
        Objects.requireNonNull(consumer);
        this.walletId = walletId;
        this.eventType = eventType;
        this.consumer = consumer;
    }

    @Override
    public void onNext(WebSocketEvent event) {
        boolean walletMatch = walletId == null || walletId.equals(event.getTheirWalletId());
        boolean eventTypeMatch = eventType == null || eventType.isAssignableFrom(event.getEventType());
        if (walletMatch && eventTypeMatch) {
            consumer.accept(event);
        }
        subscription.request(1);
    }
}