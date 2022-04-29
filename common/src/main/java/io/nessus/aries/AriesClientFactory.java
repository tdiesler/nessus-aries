package io.nessus.aries;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.multitenancy.WalletRecord;

public class AriesClientFactory {

    /**
     * Create a client for the base wallet
     */
    public static AriesClient baseClient() {
        return createClient(null, AgentConfiguration.defaultConfiguration());
    }
    
    /**
     * Create a client for the base wallet
     */
    public static AriesClient baseClient(AgentConfiguration config) {
        return createClient(null, config);
    }
    
    /**
     * Create a client for a multitenant wallet
     */
    public static AriesClient createClient(WalletRecord wallet) {
        return createClient(wallet, AgentConfiguration.defaultConfiguration());
    }
    
    /**
     * Create a client for a multitenant wallet
     */
    public static AriesClient createClient(WalletRecord wallet, AgentConfiguration config) {
        return AriesClient.builder()
                .url(config.getAdminUrl())
                .apiKey(config.getApiKey())
                .bearerToken(wallet != null ? wallet.getToken() : null)
                .build();
    }
    
}