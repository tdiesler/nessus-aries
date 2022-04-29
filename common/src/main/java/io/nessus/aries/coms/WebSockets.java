package io.nessus.aries.coms;

import org.hyperledger.aries.api.multitenancy.WalletRecord;

import io.nessus.aries.AgentConfiguration;
import io.nessus.aries.HttpClientFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.internal.ws.RealWebSocket;

public final class WebSockets {
    
    // Hide ctor
    private WebSockets() {}

    public static final OkHttpClient httpClient = HttpClientFactory.createHttpClient();

    public static WebSocket createWebSocket(WalletRecord wallet, WebSocketEventHandler handler) {
        return createWebSocket(AgentConfiguration.defaultConfiguration(), wallet, handler);
    }
    
    public static WebSocket createWebSocket(AgentConfiguration config, WalletRecord wallet, WebSocketEventHandler handler) {
        handler.init(wallet);
        String walletName = wallet.getSettings().getWalletName();
        WebSocket webSocket = httpClient.newWebSocket(new Request.Builder()
                .url(config.getWebSocketUrl())
                .header("X-API-Key", config.getApiKey())
                .header("Authorization", "Bearer " + wallet.getToken())
                .build(), new WebSocketListener(walletName, handler));
        return webSocket;
    }
    
    public static WebSocketEventHandler getEventHandler(WebSocket ws) {
        RealWebSocket rws = (RealWebSocket) ws;
        WebSocketListener listener = (WebSocketListener) rws.getListener$okhttp();
        return listener.getEventHandler();
    }
    
    public static void closeWebSocket(WebSocket ws) {
        getEventHandler(ws).close();
        ws.close(1000, "Going down");
    }
}