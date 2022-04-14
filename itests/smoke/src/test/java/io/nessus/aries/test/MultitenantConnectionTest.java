
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hyperledger.acy_py.generated.model.ConnectionInvitation;
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
 * Test RFC 0160: Connection Protocol with multitenant wallets
 * 
 * https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol
 */
public class MultitenantConnectionTest extends AbstractAriesTest {

    @Test
    void testMultitenantWallets() throws Exception {
        
        // Create multitenant wallets
        WalletRecord aliceWallet = createWallet("Alice").build();
        WalletRecord faberWallet = createWallet("Faber").role(ENDORSER).build();

        AriesClient alice = useWallet(aliceWallet);
        AriesClient faber = useWallet(faberWallet);
        
        // Count transitions to ACTIVE state
        CountDownLatch activeLatch = new CountDownLatch(2);
        
        WebSocket aliceWebSocket = createWebSocket(aliceWallet, new TenantAwareEventHandler() {
            @Override
            public void handleConnection(String walletId, ConnectionRecord con) {
                String sourceWallet = findWalletNameById(walletId);
                String myWalletName = aliceWallet.getSettings().getWalletName();
                String myWalletId = aliceWallet.getWalletId();
                ConnectionState state = con.getState();
                log.info("{} Connection: [@{}] [{}] {}", myWalletName, sourceWallet, state, con);
                
                if (myWalletId.equals(walletId) && ConnectionState.ACTIVE == state) {
                    log.info("{} CONNECTION ACTIVE", myWalletName);
                    activeLatch.countDown();
                }
            }
        });
        
        WebSocket faberWebSocket = createWebSocket(faberWallet, new TenantAwareEventHandler() {
            
            String serviceEndpoint;
            
            @Override
            public void handleSettings(String walletId, Settings settings) {
                String myWalletName = faberWallet.getSettings().getWalletName();
                log.info("{} Settings: [{}] {}", myWalletName, walletId, settings);
                this.serviceEndpoint = settings.getEndpoint();
            }
            
            @Override
            public void handleConnection(String walletId, ConnectionRecord con) throws IOException {
                String sourceWallet = findWalletNameById(walletId);
                String myWalletName = faberWallet.getSettings().getWalletName();
                String myWalletId = faberWallet.getWalletId();
                ConnectionState state = con.getState();
                log.info("{} Connection: [@{}] [{}] {}", myWalletName, sourceWallet, state, con);
                
                // Faber receives the connection invitation from Alice
                if (!myWalletId.equals(walletId) && ConnectionState.INVITATION == con.getState()) {
                    log.info("{} RECEIVE INVITATION", myWalletName);
                    faber.connectionsReceiveInvitation(ReceiveInvitationRequest.builder()
                      .recipientKeys(Collections.singletonList(con.getInvitationKey()))
                      .serviceEndpoint(serviceEndpoint)
                      .build(), ConnectionReceiveInvitationFilter.builder()
                          .autoAccept(true)
                          .build()).get();            
                }
                if (myWalletId.equals(walletId) && ConnectionState.ACTIVE == con.getState()) {
                    log.info("{} CONNECTION ACTIVE", myWalletName);
                    activeLatch.countDown();
                }
            }
        });
        
        try {
            
            // Alice creates a connection invitation for Faber
            CreateInvitationResponse aliceInvitationResponse = alice.connectionsCreateInvitation(CreateInvitationRequest.builder().build()).get();
            ConnectionInvitation aliceInvitation = aliceInvitationResponse.getInvitation();
            log.info("Alice: {}", aliceInvitationResponse);
            log.info("Alice: {}", aliceInvitation);

            // Await ACTIVE state for both Alice and Faber
            // Requires --auto-ping-connection otherwise Faber gets stuck in state RESPONSE
            Assertions.assertTrue(activeLatch.await(10, TimeUnit.SECONDS), "No ACTIVE connection");
            
        } finally {
            closeWebSocket(aliceWebSocket);
            closeWebSocket(faberWebSocket);
            removeWallet(aliceWallet);
            removeWallet(faberWallet);
        }
    }
}
