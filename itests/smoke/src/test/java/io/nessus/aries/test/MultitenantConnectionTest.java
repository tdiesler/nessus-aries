
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
import org.hyperledger.aries.api.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nessus.aries.common.websocket.WebSocketEventHandler;
import io.nessus.aries.common.websocket.WebSocketEventHandler.WebSocketEvent;
import io.nessus.aries.common.websocket.WebSockets;
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
        
        Map<String, ConnectionRecord> connections = new HashMap<>();
        CountDownLatch peerConnectionLatch = new CountDownLatch(2);
        
        WebSocket acmeSocket = WebSockets.createWebSocket(acmeWallet, new WebSocketEventHandler.Builder()
                .subscribe(null, Settings.class, ev -> log.info("{}: [@{}] {}", ev.getThisWalletName(), ev.getTheirWalletName(), ev.getPayload()))
                .walletRegistry(walletRegistry)
                .build());
        
        Consumer<WebSocketEvent> eventConsumer = ev -> {
            ConnectionRecord con = ev.getPayload(ConnectionRecord.class);
            log.info("{}: [@{}] {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), con.getTheirRole(), con.getState(), con);
            connections.put(ev.getTheirWalletId(), con);
            if (ConnectionState.ACTIVE == con.getState()) {
                peerConnectionLatch.countDown();
            }
        };
        
        WebSocket faberSocket = WebSockets.createWebSocket(faberWallet, new WebSocketEventHandler.Builder()
                .subscribe(aliceWallet.getWalletId(), ConnectionRecord.class, eventConsumer)
                .walletRegistry(walletRegistry)
                .build());
        
        WebSocket aliceSocket = WebSockets.createWebSocket(aliceWallet, new WebSocketEventHandler.Builder()
                .subscribe(faberWallet.getWalletId(), ConnectionRecord.class, eventConsumer)
                .walletRegistry(walletRegistry)
                .build());
        
        try {
            
            AriesClient faber = createClient(faberWallet);
            AriesClient alice = createClient(aliceWallet);

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

            Assertions.assertTrue(peerConnectionLatch.await(10, TimeUnit.SECONDS), "NO ACTIVE connections");
            
            // Verify that Faber can access their connection
            String faberConnectionId = connections.get(faberWallet.getWalletId()).getConnectionId();
            ConnectionRecord faberConnection = faber.connections().get().stream().filter(c -> c.getConnectionId().equals(faberConnectionId)).findAny().get();
            Assertions.assertEquals(ConnectionState.ACTIVE, faberConnection.getState());
            
            // Verify that Alice can access her connection
            String aliceConnectionId = connections.get(aliceWallet.getWalletId()).getConnectionId();
            ConnectionRecord aliceConnection = alice.connections().get().stream().filter(c -> c.getConnectionId().equals(aliceConnectionId)).findAny().get();
            Assertions.assertEquals(ConnectionState.ACTIVE, aliceConnection.getState());
            
        } finally {
            WebSockets.closeWebSocket(aliceSocket);
            WebSockets.closeWebSocket(faberSocket);
            WebSockets.closeWebSocket(acmeSocket);
            removeWallet(aliceWallet);
            removeWallet(faberWallet);
            removeWallet(acmeWallet);
        }
    }
}
