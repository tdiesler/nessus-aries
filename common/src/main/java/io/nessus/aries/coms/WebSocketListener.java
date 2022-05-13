package io.nessus.aries.coms;

import java.util.Objects;

import org.hyperledger.aries.webhook.AriesWebSocketListener;

public class WebSocketListener extends AriesWebSocketListener {

    private final WebSocketEventHandler handler;
    
    public WebSocketListener(String label, WebSocketEventHandler handler) {
        super(label, handler);
        Objects.nonNull(handler);
        this.handler = handler;
    }

    public WebSocketEventHandler getEventHandler() {
        return handler;
    }
}