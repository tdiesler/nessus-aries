package io.nessus.aries.wallet;

import static io.nessus.aries.AgentConfiguration.getSystemEnv;

import java.io.IOException;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.acy_py.generated.model.DIDEndpoint;
import org.hyperledger.acy_py.generated.model.TxnOrRegisterLedgerNymResponse;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.IndyLedgerRoles;
import org.hyperledger.aries.api.ledger.RegisterNymFilter;
import org.hyperledger.aries.api.multitenancy.CreateWalletRequest;
import org.hyperledger.aries.api.multitenancy.WalletDispatchType;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.api.multitenancy.WalletType;
import org.hyperledger.aries.api.wallet.WalletDIDCreate;
import org.hyperledger.aries.config.GsonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.nessus.aries.AgentConfiguration;
import io.nessus.aries.AriesClientFactory;
import io.nessus.aries.util.AssertState;

public class WalletBuilder {
    
    static final Logger log = LoggerFactory.getLogger(WalletBuilder.class);
    static final Gson gson = GsonConfig.defaultConfig();
    
    private String walletName;
    private String walletKey;
    private AgentConfiguration agentConfig = AgentConfiguration.defaultConfiguration();
    private WalletDispatchType dispatchType = WalletDispatchType.DEFAULT;
    private WalletType walletType = WalletType.INDY;
    private WalletRegistry walletRegistry;
    private WalletRecord trusteeWallet;
    private IndyLedgerRoles ledgerRole;
    private boolean selfRegister;
    
    public WalletBuilder(String walletName) {
        this.walletName = walletName;
    }

    public WalletBuilder agentConfig(AgentConfiguration agentConfig) {
        this.agentConfig = agentConfig;
        return this;
    }
    
    public WalletBuilder walletKey(String key) {
        this.walletKey = key;
        return this;
    }

    public WalletBuilder walletType(WalletType type) {
        this.walletType = type;
        return this;
    }

    public WalletBuilder dispatchType(WalletDispatchType dispatchType) {
        this.dispatchType = dispatchType;
        return this;
    }

    public WalletBuilder ledgerRole(IndyLedgerRoles role) {
        this.ledgerRole = role;
        return this;
    }
    
    public WalletBuilder selfRegisterNym(boolean flag) {
        this.selfRegister = flag;
        return this;
    }
    
    public WalletBuilder selfRegisterNym() {
        this.selfRegister = true;
        return this;
    }
    
    public WalletBuilder trusteeWallet(WalletRecord trusteeWallet) {
        this.trusteeWallet = trusteeWallet;
        return this;
    }
    
    public static boolean selfRegisterWithDid(String alias, String did, String vkey, IndyLedgerRoles role) throws IOException {
        String host = getSystemEnv("INDY_WEBSERVER_HOSTNAME", "localhost");
        String port = getSystemEnv("INDY_WEBSERVER_PORT", "9000");
        return new SelfRegistrationHandler(String.format("http://%s:%s/register", host, port))
            .registerWithDID(alias, did, vkey, role);
    }

    public WalletBuilder walletRegistry(WalletRegistry walletRegistry) {
        this.walletRegistry = walletRegistry;
        return this;
    }
    
    public NessusWallet build() throws IOException {
        
        CreateWalletRequest walletRequest = CreateWalletRequest.builder()
                .walletKey(walletKey != null ? walletKey : walletName + "Key")
                .walletDispatchType(dispatchType)
                .walletName(walletName)
                .walletType(walletType)
                .build();
        log.info("CreateWalletRequest: {}", gson.toJson(walletRequest));
        
        AriesClient baseClient = AriesClientFactory.baseClient(agentConfig);
        WalletRecord walletRecord = baseClient.multitenancyWalletCreate(walletRequest).get();
        NessusWallet nessusWallet = NessusWallet.build(walletRecord).withWalletRegistry(walletRegistry);
        String walletId = nessusWallet.getWalletId();
        log.info("{}: [{}] {}", walletName, walletId, nessusWallet);

        if (ledgerRole != null) {
            
            AssertState.isTrue(selfRegister || trusteeWallet != null, "LedgerRole " + ledgerRole + " requires selfRegister or trusteeWallet");
            
            // Create a local DID for the wallet
            AriesClient client = AriesClientFactory.createClient(nessusWallet, agentConfig);
            DID did = client.walletDidCreate(WalletDIDCreate.builder().build()).get();
            log.info("{}: {}", walletName, did);
            
            if (trusteeWallet != null) {
                
                AriesClient trustee = AriesClientFactory.createClient(trusteeWallet, agentConfig);
                String trusteeName = trusteeWallet.getSettings().getWalletName();
                TxnOrRegisterLedgerNymResponse nymResponse = trustee.ledgerRegisterNym(RegisterNymFilter.builder()
                        .did(did.getDid())
                        .verkey(did.getVerkey())
                        .role(ledgerRole)
                        .build()).get();
                log.info("{} for {}: {}", trusteeName, walletName, nymResponse);
            } 
            else if (selfRegister) {
                // Register DID with the leder (out-of-band)
                selfRegisterWithDid(walletName, did.getDid(), did.getVerkey(), ledgerRole);
            }
            
            // Set the public DID for the wallet
            client.walletDidPublic(did.getDid());
            nessusWallet.setPublicDid(did);
            
            DIDEndpoint didEndpoint = client.walletGetDidEndpoint(did.getDid()).get();
            log.info("{}: {}", walletName, didEndpoint);
        } 
        
        if (walletRegistry != null)
            walletRegistry.putWallet(nessusWallet);
        
        return nessusWallet;
    }
}

