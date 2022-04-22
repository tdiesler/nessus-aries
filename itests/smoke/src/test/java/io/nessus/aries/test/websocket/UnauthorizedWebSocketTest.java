
package io.nessus.aries.test.websocket;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * WebSocket endpoint accepts unauthorized connections
 * 
 * https://github.com/hyperledger/aries-cloudagent-python/issues/1727
 */
@Disabled("[#1727] WebSocket endpoint accepts unauthorized connections")
public class UnauthorizedWebSocketTest {

    final Logger log = LoggerFactory.getLogger(getClass());
    
    @Test
    void tesUnauthorized() throws Exception {
        
        MyWebSocketListener listener = new MyWebSocketListener();
        
        OkHttpClient httpClient = new OkHttpClient.Builder().build();
        WebSocket webSocket = httpClient.newWebSocket(new Request.Builder()
                .url("ws://localhost:8031/ws")
                //.header("X-API-Key", ACAPY_API_KEY)
                //.header("Authorization", "Bearer " + token)
                .build(), listener);
        
        try {
            
            Assertions.assertFalse(listener.await(10, TimeUnit.SECONDS), "No messages expected");
            
        } finally {
            webSocket.close(1000, "Going down");
        }
    }
    
    class MyWebSocketListener extends WebSocketListener {
        
        CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            log.info("Open: {}", response);
            latch.countDown();
        }

        @Override
        public void onMessage(WebSocket webSocket, String message) {
            log.info("Message: {}", message);
            latch.countDown();
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            log.info("Closing: {} {}", code, reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            log.info("Closed: {} {}", code, reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable th, Response response) {
            log.error("Failure: " + response, th);
        }

        boolean await(int timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }
}
