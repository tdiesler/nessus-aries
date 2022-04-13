
package io.nessus.aries.test.websocket;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hyperledger.aries.webhook.WebSocketEventHandler;
import org.hyperledger.aries.webhook.WebSocketListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.nessus.aries.test.AbstractAriesTest;
import okhttp3.Request;
import okhttp3.WebSocket;

/**
 * WebSocket endpoint accepts unauthorized connections
 * 
 * https://github.com/hyperledger/aries-cloudagent-python/issues/1727
 */
@Disabled("[#1727] WebSocket endpoint accepts unauthorized connections")
public class UnauthorizedWebSocketTest extends AbstractAriesTest {

    @Test
    void tesUnauthorized() throws Exception {
        
        CountDownLatch latch = new CountDownLatch(2);
        
        WebSocketEventHandler handler = new WebSocketEventHandler() {
            @Override
            public void handleRaw(String topic, String message) {
                log.info("{}: {}", topic, message);
                latch.countDown();
            }
        };
        
        WebSocket webSocket = getHttpClient().newWebSocket(new Request.Builder()
                .url("ws://localhost:8031/ws")
                //.header("X-API-Key", ACAPY_API_KEY)
                //.header("Authorization", "Bearer " + token)
                .build(), new WebSocketListener(null, handler));
        
        try {
            
            Assertions.assertFalse(latch.await(10, TimeUnit.SECONDS), "No messages expected");
            
        } finally {
            closeWebSocket(webSocket);
        }
    }
}
