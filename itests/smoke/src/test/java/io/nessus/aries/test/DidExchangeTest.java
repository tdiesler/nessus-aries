
package io.nessus.aries.test;

import static io.nessus.aries.common.WebSockets.closeWebSocket;
import static io.nessus.aries.common.WebSockets.createWebSocket;
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.util.concurrent.CountDownLatch;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.did_exchange.DidExchangeAcceptRequestFilter;
import org.hyperledger.aries.api.did_exchange.DidExchangeCreateRequestFilter;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.nessus.aries.common.WebSocketEventHandler;
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
        
        CountDownLatch activeLatch = new CountDownLatch(2);
        class WSEHandler extends WebSocketEventHandler  {
            
            @Override
            public void handleConnection(String walletId, ConnectionRecord con) throws Exception {
                super.handleConnection(walletId, con);
                if (ConnectionState.REQUEST == con.getState() && !thisWalletId().equals(walletId)) {
                    log.info("{} ACCEPT REQUEST", thisWalletName());
                    createClient().didExchangeAcceptRequest(con.getConnectionId(), DidExchangeAcceptRequestFilter.builder().build()).get();
                }
                if (ConnectionState.ACTIVE == con.getState() && thisWalletId().equals(walletId)) {
                    log.info("{} CONNECTION ACTIVE", thisWalletName());
                    activeLatch.countDown();
                }
            }
        };
        
        WebSocket inviterSocket = createWebSocket(faberWallet, new WSEHandler());
        WebSocket inviteeSocket = createWebSocket(aliceWallet, new WSEHandler());
        
        try {
            AriesClient faber = createClient(faberWallet);
            AriesClient alice = createClient(aliceWallet);

            String faberPublicDid = faber.walletDidPublic().get().getDid();
            ConnectionRecord con = alice.didExchangeCreateRequest(DidExchangeCreateRequestFilter.builder()
                    .theirPublicDid(faberPublicDid)
                    .build()).get();
            log.info("{}", con);

            //Assertions.assertTrue(activeLatch.await(10, TimeUnit.SECONDS), "NO ACTIVE connections");
            
        } finally {
            closeWebSocket(inviterSocket);
            closeWebSocket(inviteeSocket);
            removeWallet(faberWallet);
            removeWallet(aliceWallet);
        }
    }
}
