package io.nessus.aries.wallet;

import java.io.IOException;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.acy_py.generated.model.DIDCreate;
import org.hyperledger.acy_py.generated.model.DIDEndpoint;
import org.hyperledger.acy_py.generated.model.RegisterLedgerNymResponse;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.IndyLedgerRoles;
import org.hyperledger.aries.api.ledger.RegisterNymFilter;
import org.hyperledger.aries.api.multitenancy.CreateWalletRequest;
import org.hyperledger.aries.api.multitenancy.WalletDispatchType;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.api.multitenancy.WalletType;
import org.hyperledger.aries.config.GsonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.nessus.aries.AgentConfiguration;
import io.nessus.aries.AriesClientFactory;

public class WalletBuilder {
    
    static final Logger log = LoggerFactory.getLogger(WalletBuilder.class);
    static final Gson gson = GsonConfig.defaultConfig();
    
    public static final WalletRegistry walletRegistry = new WalletRegistry();

    private String walletName;
    private String walletKey;
    private AgentConfiguration agentConfig = AgentConfiguration.defaultConfiguration();
    private WalletDispatchType dispatchType = WalletDispatchType.DEFAULT;
    private WalletType walletType = WalletType.INDY;
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
    
    public static boolean selfRegisterWithDid(String alias, String did, String vkey, IndyLedgerRoles role) throws IOException {
        return new SelfRegistrationHandler("http://localhost:9000/register")
            .registerWithDID(alias, did, vkey, role);
    }

    public WalletRecord build() throws IOException {
        
        CreateWalletRequest walletRequest = CreateWalletRequest.builder()
                .walletKey(walletKey != null ? walletKey : walletName + "Key")
                .walletDispatchType(dispatchType)
                .walletName(walletName)
                .walletType(walletType)
                .build();
        log.info("CreateWalletRequest: {}", gson.toJson(walletRequest));
        
        AriesClient baseClient = AriesClientFactory.baseClient(agentConfig);
        WalletRecord walletRecord = baseClient.multitenancyWalletCreate(walletRequest).get();
        String walletId = walletRecord.getWalletId();
        log.info("{}: [{}] {}", walletName, walletId, walletRecord);

        String accessToken = walletRecord.getToken();
        log.info("{} Token: {}", walletName, accessToken);

        if (selfRegister || trusteeWallet != null) {
            
            DID did = null;
            AriesClient client = AriesClientFactory.createClient(walletRecord, agentConfig);
            
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
                
                AriesClient trustee = AriesClientFactory.createClient(trusteeWallet, agentConfig);
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

