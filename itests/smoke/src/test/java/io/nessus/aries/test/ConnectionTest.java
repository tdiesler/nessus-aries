
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.io.IOException;
import java.util.List;

import org.hyperledger.acy_py.generated.model.ConnectionInvitation;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionReceiveInvitationFilter;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.connection.CreateInvitationRequest;
import org.hyperledger.aries.api.connection.CreateInvitationResponse;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test the Acapy wallet endpoint
 */
public class ConnectionTest extends AbstractAriesTest {

    @Test
    void testMultitenantWallet() throws Exception {

        // Create multitenant wallets
        WalletRecord aliceWallet = createWalletWithDID("Alice", "keyA", null);
        WalletRecord faberWallet = createWalletWithDID("Faber", "keyB", ENDORSER);
        
        try {
            AriesClient alice = useWallet(aliceWallet.getToken());
            AriesClient faber = useWallet(faberWallet.getToken());
            
            // Faber creates a connection invitation for Alice
            CreateInvitationResponse faberInvitationResponse = faber.connectionsCreateInvitation(CreateInvitationRequest.builder().build()).get();
            ConnectionInvitation faberInvitation = faberInvitationResponse.getInvitation();
            String faberConnectionId = faberInvitationResponse.getConnectionId();
            log.info("Faber: {}", faberInvitationResponse);
            log.info("Faber: {}", faberInvitation);
            
            assertConnectionState(faberWallet, ConnectionState.INVITATION);
            
            // Alice receives the connection invitation from Faber
            alice.connectionsReceiveInvitation(ReceiveInvitationRequest.builder()
                    .recipientKeys(faberInvitation.getRecipientKeys())
                    .serviceEndpoint(faberInvitation.getServiceEndpoint())
                    .build(), ConnectionReceiveInvitationFilter.builder()
                        .autoAccept(true)
                        .build()).get();
            
            // Faber awaits REQUEST state
            awaitConnectionState(faberWallet, ConnectionState.REQUEST);

            // Faber accepts the connection request
            faber.connectionsAcceptRequest(faberConnectionId, null);
            
            // Faber awaits ACTIVE state
            awaitConnectionState(faberWallet, ConnectionState.ACTIVE);

            // Alice awaits ACTIVE state
            awaitConnectionState(aliceWallet, ConnectionState.ACTIVE);
            
        } finally {
            removeWallet(aliceWallet.getWalletId(), "keyA");
            removeWallet(faberWallet.getWalletId(), "keyB");
        }
    }

    private ConnectionRecord assertConnectionState(WalletRecord wallet, ConnectionState targetState) throws IOException {
        String walletName = wallet.getSettings().getWalletName();
        AriesClient client = useWallet(wallet.getToken());
        List<ConnectionRecord> records = client.connections().get();
        Assertions.assertEquals(1, records.size(), walletName + ": Unexpected number of connection records");
        ConnectionRecord rec = records.get(0);
        String id = rec.getConnectionId();
        ConnectionState state = rec.getState();
        log.info("{}: cid={} state={} - {}", walletName, id, state, rec);
        Assertions.assertEquals(targetState, state, walletName + ": Unexpected connection state");
        return rec;
    }
    
    private ConnectionRecord awaitConnectionState(WalletRecord wallet, ConnectionState targetState) throws Exception {
        String walletName = wallet.getSettings().getWalletName();
        AriesClient client = useWallet(wallet.getToken());
        for (int i = 0; i < 10; i++) {
            List<ConnectionRecord> records = client.connections().get();
            if (records.isEmpty()) log.info("{}: No connection records");
            for (ConnectionRecord rec : records) {
                String id = rec.getConnectionId();
                ConnectionState state = rec.getState();
                log.info("{}: cid={} state={} - {}", walletName, id, state, rec);
                if (state == targetState) 
                    return rec;
            }
            Thread.sleep(2000);
        }
        throw new RuntimeException(String.format("%s: %s connection state not reached", walletName, targetState));
    }
}
