
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.time.Duration;
import java.util.List;

import org.hyperledger.acy_py.generated.model.ConnectionInvitation;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.AriesWebSocketClient;
import org.hyperledger.aries.api.connection.ConnectionReceiveInvitationFilter;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.connection.CreateInvitationRequest;
import org.hyperledger.aries.api.connection.CreateInvitationResponse;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nessus.aries.wallet.NessusWallet;

/**
 * Test RFC 0160: Connection Protocol with multitenant wallets
 * 
 * https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol
 */
public class PeerConnectionTest extends AbstractAriesTest {

    @Test
    void testMultitenantWallets() throws Exception {
        
        // Create multitenant wallets
        NessusWallet inviterWallet = createWallet("Faber")
                .ledgerRole(ENDORSER).selfRegisterNym().build();
        
        // Alice does not have a public DID
        NessusWallet inviteeWallet = createWallet("Alice").build();
                
        try {
            
            logSection("Connect Faber to Alice");

            AriesClient inviter = createClient(inviterWallet);
            AriesClient invitee = createClient(inviteeWallet);
            
            AriesWebSocketClient inviterWSClient = inviterWallet.getWebSocketClient();            
            AriesWebSocketClient inviteeWSClient = inviteeWallet.getWebSocketClient();            
            
            // Inviter creates an invitation (/connections/create-invitation)
            CreateInvitationResponse response = inviter.connectionsCreateInvitation(CreateInvitationRequest.builder().build()).get();
            ConnectionInvitation invitation = response.getInvitation();
            
            // Invitee receives the invitation from the Inviter (/connections/receive-invitation)
            invitee.connectionsReceiveInvitation(ReceiveInvitationRequest.builder()
                    .recipientKeys(invitation.getRecipientKeys())
                    .serviceEndpoint(invitation.getServiceEndpoint())
                    .build(), ConnectionReceiveInvitationFilter.builder()
                        .autoAccept(true)
                        .build()).get();

            ConnectionRecord inviterConnectionRecord = inviterWSClient.connection()
                    .filter(ConnectionRecord::stateIsActive)
                    .blockFirst(Duration.ofSeconds(10));
            
            ConnectionRecord inviteeConnectionRecord = inviteeWSClient.connection()
                    .filter(ConnectionRecord::stateIsActive)
                    .blockFirst(Duration.ofSeconds(10));
            
            String inviterConnectionId = inviterConnectionRecord.getConnectionId();
            String inviteeConnectionId = inviteeConnectionRecord.getConnectionId();

            // Verify that Faber can access their connection
            List<ConnectionRecord> faberConnections = inviter.connections().get();
            faberConnections.stream().forEach(con -> log.info("Faber: {}", con));
            Assertions.assertEquals(inviterConnectionId, faberConnections.get(0).getConnectionId());
            Assertions.assertEquals(ConnectionState.ACTIVE, faberConnections.get(0).getState());
            
            // Verify that Alice can access her connection
            List<ConnectionRecord> aliceConnections = invitee.connections().get();
            aliceConnections.stream().forEach(con -> log.info("Alice: {}", con));
            Assertions.assertEquals(inviteeConnectionId, aliceConnections.get(0).getConnectionId());
            Assertions.assertEquals(ConnectionState.ACTIVE, aliceConnections.get(0).getState());
            
        } finally {
            inviterWallet.closeAndRemove();
            inviteeWallet.closeAndRemove();
        }
    }
}
