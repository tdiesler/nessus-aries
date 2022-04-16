
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hyperledger.acy_py.generated.model.ConnectionInvitation;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionReceiveInvitationFilter;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.connection.CreateInvitationParams;
import org.hyperledger.aries.api.connection.CreateInvitationRequest;
import org.hyperledger.aries.api.connection.CreateInvitationResponse;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nessus.aries.common.WebSocketEventHandler;
import okhttp3.WebSocket;

/**
 * Test RFC 0160: Connection Protocol with multitenant wallets
 * 
 * https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol
 */
public class MultitenantConnectionTest extends AbstractAriesTest {

    @Test
    void testMultitenantWallets() throws Exception {
        
        // Create multitenant wallets
        WalletRecord acmeWallet = new WalletBuilder("Acme")
                .ledgerRole(ENDORSER).selfRegisterNym().build();
        
        WalletRecord faberWallet = new WalletBuilder("Faber")
                .ledgerRole(ENDORSER).selfRegisterNym().build();
        
        // Alice does not have a public DID
        WalletRecord aliceWallet = new WalletBuilder("Alice").build();
        
        CountDownLatch activeLatch = new CountDownLatch(2);
        class MyWebSocketEventHandler extends WebSocketEventHandler {
            
            MyWebSocketEventHandler(WalletRecord myWallet) {
                super(myWallet);
            }
            
            @Override
            public void handleConnection(String walletId, ConnectionRecord con) throws Exception {
                super.handleConnection(walletId, con);
                if (ConnectionState.ACTIVE == con.getState() && myWalletId.equals(walletId)) {
                    log.info("{} CONNECTION ACTIVE", myWalletName);
                    activeLatch.countDown();
                }
            }
        }
        
        WebSocket acmeWebSocket = createWebSocket(acmeWallet, new WebSocketEventHandler(acmeWallet) {
        });
        
        WebSocket faberWebSocket = createWebSocket(faberWallet, new MyWebSocketEventHandler(faberWallet) {
        });
        
        WebSocket aliceWebSocket = createWebSocket(aliceWallet, new MyWebSocketEventHandler(aliceWallet) {
        });
        
        try {
            
            AriesClient alice = createClient(aliceWallet);
            AriesClient faber = createClient(faberWallet);
            
            log.info("===================================================================================");
            
            // Faber creates an invitation (/connections/create-invitation)
            CreateInvitationResponse createInvitationResponse = faber.connectionsCreateInvitation(
                    CreateInvitationRequest.builder()
                        .myLabel("Faber/Alice")
                        .build(), 
                    CreateInvitationParams.builder()
                        .autoAccept(true)
                        .build()).get();
            ConnectionInvitation invitation = createInvitationResponse.getInvitation();
            log.info("Faber: {}", createInvitationResponse);
            log.info("Faber: {}", invitation);

            // Alice receives the invitation from Faber (/connections/receive-invitation)
            alice.connectionsReceiveInvitation(ReceiveInvitationRequest.builder()
                    .recipientKeys(invitation.getRecipientKeys())
                    .serviceEndpoint(invitation.getServiceEndpoint())
                    .build(), ConnectionReceiveInvitationFilter.builder()
                        .autoAccept(true)
                        .build()).get();
            
            // Await ACTIVE state for both Alice and Faber
            // Requires --auto-ping-connection otherwise Faber gets stuck in state RESPONSE
            Assertions.assertTrue(activeLatch.await(10, TimeUnit.SECONDS), "No ACTIVE connection");
            
        } finally {
            closeWebSocket(aliceWebSocket);
            closeWebSocket(faberWebSocket);
            closeWebSocket(acmeWebSocket);
            removeWallet(aliceWallet);
            removeWallet(faberWallet);
            removeWallet(acmeWallet);
        }
    }

}
