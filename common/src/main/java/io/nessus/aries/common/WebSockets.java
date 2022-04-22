package io.nessus.aries.common;

import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.webhook.AriesWebSocketListener;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public final class WebSockets {
    
    // Hide ctor
    private WebSockets() {}

    public static final OkHttpClient httpClient = HttpClient.createHttpClient();

    public static WebSocket createWebSocket(WalletRecord thisWallet, WebSocketEventHandler handler) {
        handler.setThisWallet(thisWallet);
        String walletName = thisWallet.getSettings().getWalletName();
        WebSocket webSocket = httpClient.newWebSocket(new Request.Builder()
                .url("ws://localhost:8031/ws")
                .header("X-API-Key", Configuration.ACAPY_API_KEY)
                .header("Authorization", "Bearer " + thisWallet.getToken())
                .build(), new AriesWebSocketListener(walletName, handler));
        return webSocket;
    }
    
    public static void closeWebSocket(WebSocket ws) {
        ws.close(1000, "Going down");
    }
}