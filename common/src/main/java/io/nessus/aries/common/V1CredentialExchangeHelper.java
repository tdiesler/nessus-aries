package io.nessus.aries.common;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.IssueCredentialRecordsFilter;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V1CredentialExchangeHelper {
    
    final Logger log = LoggerFactory.getLogger(V1CredentialExchangeHelper.class);
    
    private final WalletRecord wallet;
    
    public V1CredentialExchangeHelper(WalletRecord wallet) {
        this.wallet = wallet;
    }

    public Optional<V1CredentialExchange> awaitV1CredentialExchange(String connectionId, String credentialDefinitionId, CredentialExchangeState targetState, int timeout, TimeUnit unit) throws Exception {
        String walletName = wallet.getSettings().getWalletName();
        AriesClient client = Configuration.createClient(wallet);
        long now = System.currentTimeMillis();
        long timeup = now + unit.toMillis(timeout);
        V1CredentialExchange[] result = new V1CredentialExchange[1];
        while (result[0] == null && now < timeup) {
            client.issueCredentialRecords(IssueCredentialRecordsFilter.builder()
                    .connectionId(connectionId)
                    .build()).get().stream().filter(ce -> ce.getCredentialDefinitionId().equals(credentialDefinitionId)).forEach(ce -> {
                        if (ce.getState() == targetState) {
                            log.info("{}: {} {} {}", walletName, ce.getRole(), ce.getState(), ce);
                            result[0] = ce;
                        }
                    });
            Thread.sleep(2000);
            now = System.currentTimeMillis();
        }
        return Optional.ofNullable(result[0]);
    }
}