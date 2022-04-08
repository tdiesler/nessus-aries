package io.nessus.aries.test;

import java.io.IOException;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.acy_py.generated.model.DIDCreate;
import org.hyperledger.acy_py.generated.model.DIDEndpoint;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.IndyLedgerRoles;
import org.hyperledger.aries.api.multitenancy.CreateWalletRequest;
import org.hyperledger.aries.api.multitenancy.RemoveWalletRequest;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.api.multitenancy.WalletType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.aries.common.SelfRegistrationHandler;

abstract class AbstractAriesTest {

    static final String ACAPY_ADMIN_URL = "http://localhost:8031";
    static final String ACAPY_API_KEY = "adminkey";

    final Logger log = LoggerFactory.getLogger(getClass());

    final AriesClient client = AriesClient.builder()
            .url(ACAPY_ADMIN_URL)
            .apiKey(ACAPY_API_KEY).build();

    /**
     * Create a multitenant wallet
     */
    WalletRecord createWallet(String walletName, String walletKey) throws IOException {
        WalletRecord walletRecord = client.multitenancyWalletCreate(CreateWalletRequest.builder()
                .walletName(walletName)
                .walletKey(walletKey)
                .walletType(WalletType.INDY)
                .build()).get();
        return walletRecord;
    }

    WalletRecord createWalletWithDID(String walletName, String walletKey, IndyLedgerRoles role) throws IOException {
        
        WalletRecord walletRecord = createWallet(walletName, walletKey);
        String accessToken = walletRecord.getToken();
        log.info("{}: {}", walletName, accessToken);

        // Create client for sub wallet
        AriesClient client = useWallet(accessToken);

        DID did = client.walletDidCreate(DIDCreate.builder().build()).get();
        log.info("{}: {}", walletName, did);
        
        // Register DID with the leder (out-of-band)
        selfRegisterDid(did.getDid(), did.getVerkey(), role);
        
        // Set the public DID for the wallet
        client.walletDidPublic(did.getDid());
        
        DIDEndpoint didEndpoint = client.walletGetDidEndpoint(did.getDid()).get();
        log.info("{}: {}", walletName, didEndpoint);
        
        return walletRecord;
    }
    
    /**
     * Get the multitenant wallet for the given Id
     */
    WalletRecord getWallet(String walletId) throws IOException {
        WalletRecord walletRecord = client.multitenancyWalletGet(walletId).get();
        return walletRecord;
    }

    /**
     * Create a client for a multitenant wallet
     */
    AriesClient useWallet(String walletToken) throws IOException {
        AriesClient walletClient = AriesClient.builder()
                .url(ACAPY_ADMIN_URL)
                .apiKey(ACAPY_API_KEY)
                .bearerToken(walletToken)
                .build();
        return walletClient;
    }

    /**
     * Remove a multitenant wallet
     */
    void removeWallet(String walletId, String walletKey) throws IOException {
        if (walletId != null) {
            client.multitenancyWalletRemove(walletId, RemoveWalletRequest.builder()
                    .walletKey(walletKey).build());
        }
    }

    void selfRegisterDid(String did, String vkey, IndyLedgerRoles role) throws IOException {
        new SelfRegistrationHandler("http://localhost:9000/register")
            .registerDID(did, vkey, role);
    }
}
