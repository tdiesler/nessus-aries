package io.nessus.aries.coms;

import org.hyperledger.aries.api.multitenancy.WalletRecord;

import io.nessus.aries.Configuration;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.internal.ws.RealWebSocket;

public final class WebSockets {
    
    // Hide ctor
    private WebSockets() {}

    public static final OkHttpClient httpClient = HttpClient.createHttpClient();

    public static WebSocket createWebSocket(WalletRecord thisWallet, WebSocketEventHandler handler) {
        handler.init(thisWallet);
        String walletName = thisWallet.getSettings().getWalletName();
        WebSocket webSocket = httpClient.newWebSocket(new Request.Builder()
                .url("ws://localhost:8031/ws")
                .header("X-API-Key", Configuration.ACAPY_API_KEY)
                .header("Authorization", "Bearer " + thisWallet.getToken())
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