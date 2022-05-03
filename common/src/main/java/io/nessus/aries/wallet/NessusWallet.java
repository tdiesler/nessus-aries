package io.nessus.aries.wallet;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.multitenancy.RemoveWalletRequest;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.config.GsonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.nessus.aries.AriesClientFactory;
import io.nessus.aries.coms.WebSocketEventHandler;
import io.nessus.aries.coms.WebSockets;
import okhttp3.WebSocket;

public class NessusWallet extends WalletRecord implements Closeable {

    static final Logger log = LoggerFactory.getLogger(WalletRecord.class);
    
    static final Gson gson = GsonConfig.defaultConfig();
    
    private WalletRegistry walletRegistry;
    private WebSocket webSocket;
    
    public static NessusWallet build(WalletRecord wr) {
        String json = gson.toJson(wr);
        NessusWallet wallet = gson.fromJson(json, NessusWallet.class);
        return wallet;
    }
    
    public NessusWallet withWalletRegistry(WalletRegistry walletRegistry) {
        this.walletRegistry = walletRegistry;
        return this;
    }
    
    public String getWalletName() {
        return getSettings().getWalletName();
    }

    public WalletRegistry getWalletRegistry() {
        return walletRegistry;
    }

    public synchronized boolean hasWebSocket() {
        return webSocket == null;
    }

    public synchronized WebSocket getWebSocket() {
        if (webSocket == null) {
            webSocket = WebSockets.createWebSocket_(this, new WebSocketEventHandler.Builder()
                    .subscribe(Arrays.asList(), ev -> log.debug("{}: [@{}] {}", ev.getThisWalletName(), ev.getTheirWalletName(), ev.getPayload()))
                    .walletRegistry(walletRegistry)
                    .build());
        }
        return webSocket;
    }

    public synchronized void closeWebSocket() {
        WebSockets.closeWebSocket(webSocket);
        webSocket = null;
    }
    
    public WebSocketEventHandler getWebSocketEventHandler() {
        return WebSockets.getEventHandler(getWebSocket());
    }
    
    @Override
    public void close() throws IOException {
        log.info("Close Wallet: {}", getWalletName());
        WebSockets.closeWebSocket(webSocket);
        AriesClient baseClient = AriesClientFactory.baseClient();
        baseClient.multitenancyWalletRemove(getWalletId(), RemoveWalletRequest.builder()
                .walletKey(getToken())
                .build());

        // Wait for the wallet to get removed 
        sleepWell(500); 
        while (!baseClient.multitenancyWallets(getWalletName()).get().isEmpty()) {
            sleepWell(500); 
        }
    }

    public static void sleepWell(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            // ignore
        }
    }
}