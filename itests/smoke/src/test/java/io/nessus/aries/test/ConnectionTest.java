
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hyperledger.acy_py.generated.model.ConnectionInvitation;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionReceiveInvitationFilter;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.connection.CreateInvitationRequest;
import org.hyperledger.aries.api.connection.CreateInvitationResponse;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.webhook.WebSocketEventHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import okhttp3.WebSocket;

/**
 * Test RFC 0160: Connection Protocol with multitenant wallets
 * 
 * https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol
 */
public class ConnectionTest extends AbstractAriesTest {

    @Test
    void testMultitenantWallet() throws Exception {
        
        // Create multitenant wallets
        WalletRecord aliceWallet = createWallet("Alice").build();
        WalletRecord faberWallet = createWallet("Faber").role(ENDORSER).build();
        
        AriesClient alice = useWallet(aliceWallet);
        AriesClient faber = useWallet(faberWallet);
        
        // [TODO] Faber receives the invitation event multiple times
        AtomicBoolean faberReceivedInvitation = new AtomicBoolean();
        
        CountDownLatch faberActiveLatch = new CountDownLatch(1);
        WebSocket faberWebSocket = createWebSocket(faberWallet, new WebSocketEventHandler() {
            
            // [TODO] Get the service enpoint from the settings event
            String serviceEndpoint = "http://localhost:8030";
            
            @Override
            public void handleConnection(ConnectionRecord con) throws Exception {
                String walletName = faberWallet.getSettings().getWalletName();
                log.info("{} Connection Event: {}", walletName, con);
                
                // Faber receives the connection invitation from Alice
                if (ConnectionState.INVITATION == con.getState() && !faberReceivedInvitation.getAndSet(true)) {
                    faber.connectionsReceiveInvitation(ReceiveInvitationRequest.builder()
                      .recipientKeys(Collections.singletonList(con.getInvitationKey()))
                      .serviceEndpoint(serviceEndpoint)
                      .build(), ConnectionReceiveInvitationFilter.builder()
                          .autoAccept(true)
                          .build()).get();            
                }
                
                if (ConnectionState.ACTIVE == con.getState()) {
                    faberActiveLatch.countDown();
                }
            }
        });
        
        try {
            // Alice creates a connection invitation for Faber
            CreateInvitationResponse aliceInvitationResponse = alice.connectionsCreateInvitation(CreateInvitationRequest.builder().build()).get();
            ConnectionInvitation aliceInvitation = aliceInvitationResponse.getInvitation();
            log.info("Alice: {}", aliceInvitationResponse);
            log.info("Alice: {}", aliceInvitation);

            // Faber awaits ACTIVE state
            // Requires --auto-ping-connection otherwise Faber gets stuck in state RESPONSE
            Assertions.assertTrue(faberActiveLatch.await(10, TimeUnit.SECONDS), "No ACTIVE connection");
            
        } finally {
            closeWebSocket(faberWebSocket);
            removeWallet(aliceWallet);
            removeWallet(faberWallet);
        }
    }
}
