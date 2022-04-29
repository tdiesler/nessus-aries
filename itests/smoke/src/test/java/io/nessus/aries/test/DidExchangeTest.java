
package io.nessus.aries.test;

import static io.nessus.aries.coms.WebSockets.createWebSocket;
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.did_exchange.DidExchangeCreateRequestFilter;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.api.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.nessus.aries.coms.WebSocketEventHandler;
import io.nessus.aries.wallet.WalletBuilder;
import okhttp3.WebSocket;

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
        WalletRecord faberWallet = new WalletBuilder("Faber")
                .ledgerRole(ENDORSER).selfRegisterNym().build();
        
        // Alice does not have a public DID
        WalletRecord aliceWallet = new WalletBuilder("Alice").build();
                
        log.info("===================================================================================");
        
        //Map<String, ConnectionRecord> connections = new HashMap<>();
        CountDownLatch peerConnectionLatch = new CountDownLatch(2);
        
        WebSocket inviterSocket = createWebSocket(faberWallet, new WebSocketEventHandler.Builder()
                .subscribe(Settings.class, ev -> log.info("{}: [@{}] {}", ev.getThisWalletName(), ev.getTheirWalletName(), ev.getPayload()))
                .walletRegistry(getWalletRegistry())
                .build());
        
        WebSocket inviteeSocket = createWebSocket(aliceWallet, new WebSocketEventHandler.Builder()
                .subscribe(Settings.class, ev -> log.info("{}: [@{}] {}", ev.getThisWalletName(), ev.getTheirWalletName(), ev.getPayload()))
                .walletRegistry(getWalletRegistry())
                .build());
        
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
            closeWebSocket(inviterSocket);
            closeWebSocket(inviteeSocket);
            removeWallet(faberWallet);
            removeWallet(aliceWallet);
        }
    }
}
