package io.nessus.aries.events;

import io.nessus.aries.coms.EventSubscriber;
import io.nessus.aries.coms.WebSocketEventHandler;
import io.nessus.aries.coms.WebSocketEventHandler.WebSocketEvent;
import io.nessus.aries.util.SafeConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEventConsumer<T> implements SafeConsumer<WebSocketEvent> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final Class<T> type;

    private EventSubscriber<WebSocketEvent> subscriber;
    private T payload;

    public AbstractEventConsumer(Class<T> type) {
        this.type = type;
    }

    public Class<T> getType() {
        return type;
    }

    public T getPayload() {
        return payload;
    }

    public void cancelSubscription() {
        subscriber.cancelSubscription();
    }

    public void subscribeTo(WebSocketEventHandler eventHandler) {
        subscriber = eventHandler.subscribe(getType(), this);
    }

    @Override
    public void accept(WebSocketEvent ev) throws Exception {
        payload = ev.getPayload(type);
    }
}
