package io.nessus.aries.coms;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.nessus.aries.coms.WebSocketEventHandler.WebSocketEvent;
import io.nessus.aries.util.SafeConsumer;

public class FilteringEventSubscriber extends EventSubscriber<WebSocketEvent> {

    private final List<String> walletIds;
    private final List<Class<?>> eventTypes;
    private final SafeConsumer<WebSocketEvent> consumer;
    
    public FilteringEventSubscriber(String walletId, Class<?> eventType, SafeConsumer<WebSocketEvent> consumer) {
        this(walletId != null ? Arrays.asList(walletId) : Arrays.asList(), eventType != null ? Arrays.asList(eventType) : Arrays.asList(), consumer);
    }

    public FilteringEventSubscriber(List<String> walletIds, List<Class<?>> eventTypes, SafeConsumer<WebSocketEvent> consumer) {
        Objects.requireNonNull(walletIds);
        Objects.requireNonNull(eventTypes);
        Objects.requireNonNull(consumer);
        this.walletIds = walletIds;
        this.eventTypes = eventTypes;
        this.consumer = consumer;
    }

    @Override
    public void onNext(WebSocketEvent event) {
        boolean walletMatch = walletIds.isEmpty() || walletIds.contains(event.getTheirWalletId());
        boolean eventTypeMatch = eventTypes.isEmpty() || !eventTypes.stream().filter(et -> et.isAssignableFrom(event.getEventType())).findAny().isEmpty();
        if (walletMatch && eventTypeMatch) {
            try {
                consumer.accept(event);
            } catch (RuntimeException rte) {
                throw rte;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        subscription.request(1);
    }
}