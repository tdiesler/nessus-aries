package io.nessus.aries.test;

import java.io.IOException;

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
    WalletRecord createWallet(String walletName, String walletKey) {
        try {
            WalletRecord walletRecord = client.multitenancyWalletCreate(CreateWalletRequest.builder()
                    .walletName(walletName)
                    .walletKey(walletKey)
                    .walletType(WalletType.INDY)
                    .build()).get();
            return walletRecord;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the multitenant wallet for the given Id
     */
    WalletRecord getWallet(String walletId) {
        try {
            WalletRecord walletRecord = client.multitenancyWalletGet(walletId).get();
            return walletRecord;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a client for a multitenant wallet
     */
    AriesClient createWalletClient(String walletToken) {
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
    void removeWallet(String walletId, String walletKey) {
        if (walletId != null) {
            try {
                client.multitenancyWalletRemove(walletId, RemoveWalletRequest.builder()
                        .walletKey(walletKey).build());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void selfRegisterDid(String did, String vkey, IndyLedgerRoles role) throws IOException {
        new SelfRegistrationHandler("http://localhost:9000/register")
            .registerDID(did, vkey, role);
    }
}
