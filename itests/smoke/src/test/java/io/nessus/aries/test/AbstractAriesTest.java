package io.nessus.aries.test;

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
import io.nessus.aries.HttpClientFactory;
import io.nessus.aries.coms.WebSockets;
import io.nessus.aries.wallet.WalletBuilder;
import io.nessus.aries.wallet.WalletRegistry;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;

public abstract class AbstractAriesTest {

    public final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final OkHttpClient httpClient = HttpClientFactory.createHttpClient();

    public static final AriesClient baseClient = AriesClientFactory.baseClient();
    public static final Gson gson = GsonConfig.defaultConfig();

    /**
     * Create a client for a multitenant wallet
     */
    public AriesClient createClient(WalletRecord wallet) throws IOException {
        return AriesClientFactory.createClient(wallet);
    }

    public WalletRegistry getWalletRegistry() {
        return WalletBuilder.walletRegistry;
    }
    
    public void closeWebSocket(WebSocket wsocket) {
        if (wsocket != null) {
            WebSockets.closeWebSocket(wsocket);
        }
    }
    
    public void removeWallet(WalletRecord wallet) throws IOException {
        if (wallet != null) {
            String walletId = wallet.getWalletId();
            String walletName = wallet.getSettings().getWalletName();
            log.info("{} Remove Wallet", walletName);
            getWalletRegistry().removeWallet(walletId);
            baseClient.multitenancyWalletRemove(walletId, RemoveWalletRequest.builder()
                    .walletKey(wallet.getToken())
                    .build());

            // Wait for the wallet to get removed 
            sleepWell(500); 
            while (!baseClient.multitenancyWallets(walletName).get().isEmpty()) {
                sleepWell(1000); 
            }
        }
    }

    public void sleepWell(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            // ignore
        }
    }
    
    public void logSection(String title) {
        int len = 119 - title.length();
        char[] tail = new char[len];
        Arrays.fill(tail, '=');
        log.info("{} {}", title, String.valueOf(tail));
    }
}
