package io.nessus.aries.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.acy_py.generated.model.DIDCreate;
import org.hyperledger.acy_py.generated.model.DIDEndpoint;
import org.hyperledger.acy_py.generated.model.RegisterLedgerNymResponse;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.ledger.IndyLedgerRoles;
import org.hyperledger.aries.api.ledger.RegisterNymFilter;
import org.hyperledger.aries.api.multitenancy.CreateWalletRequest;
import org.hyperledger.aries.api.multitenancy.RemoveWalletRequest;
import org.hyperledger.aries.api.multitenancy.WalletDispatchType;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.api.multitenancy.WalletType;
import org.hyperledger.aries.config.GsonConfig;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.nessus.aries.common.Configuration;
import io.nessus.aries.common.HttpClient;
import io.nessus.aries.common.SelfRegistrationHandler;
import io.nessus.aries.common.WalletRegistry;
import okhttp3.OkHttpClient;

public abstract class AbstractAriesTest {

    public final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final OkHttpClient httpClient = HttpClient.createHttpClient();

    public static final AriesClient baseClient = AriesClient.builder()
            .url(Configuration.ACAPY_ADMIN_URL)
            .apiKey(Configuration.ACAPY_API_KEY)
            .client(httpClient)
            .build();

    public static final Gson gson = GsonConfig.defaultConfig();

    protected final WalletRegistry walletRegistry = new WalletRegistry();
    
    /**
     * Create a client for a multitenant wallet
     */
    public AriesClient createClient(WalletRecord wallet) throws IOException {
        return Configuration.createClient(wallet);
    }

    public boolean selfRegisterWithDid(String alias, String did, String vkey, IndyLedgerRoles role) throws IOException {
        return new SelfRegistrationHandler("http://localhost:9000/register")
            .registerWithDID(alias, did, vkey, role);
    }

    public DID selfRegisterWithSeed(String alias, String seed, IndyLedgerRoles role) throws IOException {
        return new SelfRegistrationHandler("http://localhost:9000/register")
            .registerWithSeed(alias, seed, role);
    }

    public ConnectionRecord getConnectionRecord(WalletRecord wallet) throws IOException {
        String walletName = wallet.getSettings().getWalletName();
        AriesClient client = createClient(wallet);
        List<ConnectionRecord> records = client.connections().get();
        Assertions.assertEquals(1, records.size(), walletName + ": Unexpected number of connection records");
        return records.get(0);
    }
    
    public ConnectionRecord assertConnectionState(WalletRecord wallet, ConnectionState targetState) throws IOException {
        String walletName = wallet.getSettings().getWalletName();
        ConnectionRecord rec = getConnectionRecord(wallet);
        String id = rec.getConnectionId();
        ConnectionState state = rec.getState();
        log.info("{}: cid={} state={} - {}", walletName, id, state, rec);
        Assertions.assertEquals(targetState, state, walletName + ": Unexpected connection state");
        return rec;
    }
    
    public void removeWallet(WalletRecord wallet) throws IOException {
        if (wallet != null) {
            String walletId = wallet.getWalletId();
            String walletName = wallet.getSettings().getWalletName();
            log.info("{} Remove Wallet", walletName);
            walletRegistry.removeWallet(walletId);
            baseClient.multitenancyWalletRemove(walletId, RemoveWalletRequest.builder()
                    .walletKey(wallet.getToken())
                    .build());

            // Wait for the wallet to get removed 
            safeSleep(500); 
            while (!baseClient.multitenancyWallets(walletName).get().isEmpty()) {
                safeSleep(1000); 
            }
        }
    }

    public void safeSleep(long millis) {
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

    public class WalletBuilder {
        
        private final String walletName;
        private String walletKey;
        private WalletDispatchType dispatchType = WalletDispatchType.DEFAULT;
        private WalletType walletType = WalletType.INDY;
        private boolean selfRegister;
        private WalletRecord trusteeWallet;
        private IndyLedgerRoles ledgerRole;
        
        public WalletBuilder(String walletName) {
            this.walletName = walletName;
        }

        public WalletBuilder key(String key) {
            this.walletKey = key;
            return this;
        }

        public WalletBuilder type(WalletType type) {
            this.walletType = type;
            return this;
        }

        public WalletBuilder ledgerRole(IndyLedgerRoles role) {
            this.selfRegister = true;
            this.ledgerRole = role;
            return this;
        }
        
        public WalletBuilder selfRegisterNym() {
            this.selfRegister = true;
            return this;
        }
        
        public WalletBuilder registerNym(WalletRecord trusteeWallet) {
            this.trusteeWallet = trusteeWallet;
            return this;
        }
        
        public WalletRecord build() throws IOException {
            
            CreateWalletRequest walletRequest = CreateWalletRequest.builder()
                    .walletKey(walletKey != null ? walletKey : walletName + "Key")
                    .walletDispatchType(dispatchType)
                    .walletName(walletName)
                    .walletType(walletType)
                    .build();
            log.info("CreateWalletRequest: {}", gson.toJson(walletRequest));
            
            WalletRecord walletRecord = baseClient.multitenancyWalletCreate(walletRequest).get();
            String walletId = walletRecord.getWalletId();
            log.info("{}: [{}] {}", walletName, walletId, walletRecord);

            String accessToken = walletRecord.getToken();
            log.info("{} Token: {}", walletName, accessToken);

            if (selfRegister || trusteeWallet != null) {
                
                DID did = null;
                AriesClient client = createClient(walletRecord);
                
                if (selfRegister) {
                    
                    // Create a local DID for the wallet
                    did = client.walletDidCreate(DIDCreate.builder().build()).get();
                    log.info("{}: {}", walletName, did);
                    
                    // Register DID with the leder (out-of-band)
                    selfRegisterWithDid(walletName, did.getDid(), did.getVerkey(), ledgerRole);
                    
                } else if (trusteeWallet != null) {
                    
                    // Create a local DID for the wallet
                    did = client.walletDidCreate(DIDCreate.builder().build()).get();
                    log.info("{}: {}", walletName, did);
                    
                    AriesClient trustee = createClient(trusteeWallet);
                    String trusteeName = trusteeWallet.getSettings().getWalletName();
                    RegisterLedgerNymResponse nymResponse = trustee.ledgerRegisterNym(RegisterNymFilter.builder()
                            .did(did.getDid())
                            .verkey(did.getVerkey())
                            .role(ledgerRole)
                            .build()).get();
                    log.info("{}: {}", trusteeName, nymResponse);
                }
                
                // Set the public DID for the wallet
                client.walletDidPublic(did.getDid());
                
                DIDEndpoint didEndpoint = client.walletGetDidEndpoint(did.getDid()).get();
                log.info("{}: {}", walletName, didEndpoint);
            }
            
            walletRegistry.putWallet(walletRecord);
            return walletRecord;
        }
    }
}
