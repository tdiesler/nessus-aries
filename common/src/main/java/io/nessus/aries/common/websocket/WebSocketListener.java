package io.nessus.aries.common.websocket;

import org.hyperledger.aries.webhook.AriesWebSocketListener;

public class WebSocketListener extends AriesWebSocketListener {

    private final WebSocketEventHandler handler;
    
    public WebSocketListener(String label, WebSocketEventHandler handler) {
        super(label, handler);
        this.handler = handler;
    }

    public WebSocketEventHandler getEventHandler() {
        return handler;
    }
}