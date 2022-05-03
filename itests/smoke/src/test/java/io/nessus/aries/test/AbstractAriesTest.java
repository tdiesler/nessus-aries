package io.nessus.aries.test;

import java.io.IOException;
import java.util.Arrays;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.config.GsonConfig;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.nessus.aries.AriesClientFactory;
import io.nessus.aries.util.AttachmentKey;
import io.nessus.aries.util.AttachmentSupport;
import io.nessus.aries.wallet.NessusWallet;
import io.nessus.aries.wallet.WalletRegistry;
import okhttp3.WebSocket;

public abstract class AbstractAriesTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final Gson gson = GsonConfig.defaultConfig();
    
    public final WalletRegistry walletRegistry = new WalletRegistry();
    private final AttachmentContext attcontext = new AttachmentContext();
    
    @AfterEach
    public void afterEach() throws Exception {
        for (String name : walletRegistry.getWalletNames()) {
            NessusWallet wallet = walletRegistry.getWalletByName(name);
            wallet.close();
        }
    }
    
    /**
     * Create a client for a multitenant wallet
     */
    public AriesClient createClient(WalletRecord wallet) throws IOException {
        return AriesClientFactory.createClient(wallet);
    }

    public WalletRegistry getWalletRegistry() {
        return walletRegistry;
    }
    
    public AttachmentContext getAttachmentContext() {
        return attcontext;
    }

    public void logSection(String title) {
        int len = 119 - title.length();
        char[] tail = new char[len];
        Arrays.fill(tail, '=');
        log.info("{} {}", title, String.valueOf(tail));
    }

    // Attachment Support ===============================================================
    
    public static class AttachmentContext extends AttachmentSupport {

        public ConnectionRecord getConnection(String inviter, String invitee) {
            return getAttachment(inviter + invitee + "Connection", ConnectionRecord.class);
        }

        public NessusWallet getWallet(String name) {
            return getAttachment(name, NessusWallet.class);
        }
        
        public WebSocket getWebSocket(String name) {
            return getAttachment(name, WebSocket.class);
        }
        
        public <T> T getAttachment(String name, Class<T> type) {
            return getAttachment(new AttachmentKey<>(name, type));
        }
        
        public <T> T putAttachment(String name,  Class<T> type, T obj) {
            return putAttachment(new AttachmentKey<T>(name, type), obj);
        }
        
        public @SuppressWarnings("unchecked")
        <T> T putAttachment(String name,  T obj) {
            return putAttachment(new AttachmentKey<T>(name, (Class<T>) obj.getClass()), obj);
        }

    }
}
