package io.nessus.aries.common;

import java.io.IOException;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.multitenancy.WalletRecord;

public class Configuration {
    
    public static final String ACAPY_ADMIN_URL = "http://localhost:8031";
    public static final String ACAPY_USER_URL = "http://localhost:8030";
    public static final String ACAPY_API_KEY = "adminkey";

    /**
     * Create a client for a multitenant wallet
     */
    public static AriesClient createClient(WalletRecord wallet) throws IOException {
        return AriesClient.builder()
                .url(Configuration.ACAPY_ADMIN_URL)
                .apiKey(Configuration.ACAPY_API_KEY)
                .bearerToken(wallet.getToken())
                .build();
    }
}