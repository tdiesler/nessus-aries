
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hyperledger.acy_py.generated.model.ConnectionInvitation;
import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionReceiveInvitationFilter;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.connection.CreateInvitationRequest;
import org.hyperledger.aries.api.connection.CreateInvitationResponse;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.api.settings.Settings;
import org.hyperledger.aries.webhook.TenantAwareEventHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import okhttp3.WebSocket;

/**
 */
public class GovernmentOnboardTest extends AbstractAriesTest {

    @Test
    void testMultitenantWallets() throws Exception {
        
        WalletRecord wallet = createWallet("Government")
                //.seed("000000000000000000000000Trustee1")
                .role(ENDORSER)
                .build();

        try {
            
            // Create client for sub wallet
            AriesClient client = useWallet(wallet);
            DID publicDid = client.walletDidPublic().get();
//            Assertions.assertEquals("V4SGRU86Z58d6TV7PBUe6f", publicDid.getDid());
//            Assertions.assertEquals("GJ1SzoWzavQYfNL9XkaJdrQejfztN4XqdsiV4ct3LXKL", publicDid.getVerkey());
            
        } finally {
            removeWallet(wallet);
        }
    }
}
