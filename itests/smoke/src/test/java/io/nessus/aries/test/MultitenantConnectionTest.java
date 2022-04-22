
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
import io.nessus.aries.common.WebSockets;
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
                
        logSection("Connect Alice to Faber");
        
        AriesClient faber = createClient(faberWallet);
        AriesClient alice = createClient(aliceWallet);

        ConnectionRecord[] aliceFaberConnection = new ConnectionRecord[1];
        ConnectionRecord[] faberAliceConnection = new ConnectionRecord[1];
        CountDownLatch aliceFaberConnectionLatch = new CountDownLatch(2);
        
        class GettingStartedEventHandler extends WebSocketEventHandler {
            
            @Override
            public synchronized void handleConnection(String walletId, ConnectionRecord con) throws Exception {
                super.handleConnection(walletId, con);
                if ("Alice".equals(getTheirWalletName(walletId)) && "Faber".equals(thisWalletName())) {
                    aliceFaberConnection[0] = con;
                    if (ConnectionState.ACTIVE == con.getState()) {
                        log.info("{} CONNECTION ACTIVE", thisWalletName());
                        aliceFaberConnectionLatch.countDown();
                    }
                }
                if ("Faber".equals(getTheirWalletName(walletId)) && "Alice".equals(thisWalletName())) {
                    faberAliceConnection[0] = con;
                    if (ConnectionState.ACTIVE == con.getState()) {
                        log.info("{} CONNECTION ACTIVE", thisWalletName());
                        aliceFaberConnectionLatch.countDown();
                    }
                }
            }
        }
        
        WebSocket acmeSocket = WebSockets.createWebSocket(acmeWallet, new GettingStartedEventHandler().walletRegistry(walletRegistry));
        WebSocket faberSocket = WebSockets.createWebSocket(faberWallet, new GettingStartedEventHandler().walletRegistry(walletRegistry));
        WebSocket aliceSocket = WebSockets.createWebSocket(aliceWallet, new GettingStartedEventHandler().walletRegistry(walletRegistry));
        
        try {
            
            // Inviter creates an invitation (/connections/create-invitation)
            CreateInvitationResponse response = faber.connectionsCreateInvitation(
                    CreateInvitationRequest.builder().build(), 
                    CreateInvitationParams.builder()
                        .autoAccept(true)
                        .build()).get();
            ConnectionInvitation invitation = response.getInvitation();
            
            // Invitee receives the invitation from the Inviter (/connections/receive-invitation)
            alice.connectionsReceiveInvitation(ReceiveInvitationRequest.builder()
                    .recipientKeys(invitation.getRecipientKeys())
                    .serviceEndpoint(invitation.getServiceEndpoint())
                    .build(), ConnectionReceiveInvitationFilter.builder()
                        .autoAccept(true)
                        .build()).get();

            Assertions.assertTrue(aliceFaberConnectionLatch.await(10, TimeUnit.SECONDS));
            
            Assertions.assertEquals(ConnectionState.ACTIVE, aliceFaberConnection[0].getState());
            Assertions.assertEquals(ConnectionState.ACTIVE, faberAliceConnection[0].getState());
            
        } finally {
            WebSockets.closeWebSocket(aliceSocket);
            WebSockets.closeWebSocket(faberSocket);
            WebSockets.closeWebSocket(acmeSocket);
            removeWallet(faberWallet);
            removeWallet(aliceWallet);
        }
    }
}
