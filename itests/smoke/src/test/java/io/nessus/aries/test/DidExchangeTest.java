
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.did_exchange.DidExchangeCreateRequestFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.nessus.aries.wallet.NessusWallet;
import io.nessus.aries.wallet.WalletBuilder;

/**
 * Test RFC 0023: DID Exchange Protocol 1.0 with multitenant wallets
 * 
 * https://github.com/hyperledger/aries-rfcs/tree/main/features/0023-did-exchange
 */
@Disabled
public class DidExchangeTest extends AbstractAriesTest {

    @Test
    void testMultitenantWallets() throws Exception {
        
        // Create multitenant wallets
        NessusWallet faberWallet = new WalletBuilder("Faber")
                .ledgerRole(ENDORSER).selfRegisterNym().build();
        
        // Alice does not have a public DID
        NessusWallet aliceWallet = new WalletBuilder("Alice").build();
                
        log.info("===================================================================================");
        
        //Map<String, ConnectionRecord> connections = new HashMap<>();
        CountDownLatch peerConnectionLatch = new CountDownLatch(2);
        
        try {
            AriesClient faber = createClient(faberWallet);
            AriesClient alice = createClient(aliceWallet);

            String faberPublicDid = faber.walletDidPublic().get().getDid();
            ConnectionRecord con = alice.didExchangeCreateRequest(DidExchangeCreateRequestFilter.builder()
                    .theirPublicDid(faberPublicDid)
                    .build()).get();
            log.info("{}", con);

            Assertions.assertTrue(peerConnectionLatch.await(10, TimeUnit.SECONDS), "NO ACTIVE connections");
            
        } finally {
            faberWallet.closeAndRemove();
            aliceWallet.closeAndRemove();
        }
    }
}
