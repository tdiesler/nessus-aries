
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hyperledger.acy_py.generated.model.ConnectionInvitation;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionReceiveInvitationFilter;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.connection.CreateInvitationRequest;
import org.hyperledger.aries.api.connection.CreateInvitationResponse;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nessus.aries.events.ConnectionEventConsumer;
import io.nessus.aries.util.AssertState;
import io.nessus.aries.wallet.NessusWallet;

/**
 * Test RFC 0160: Connection Protocol with multitenant wallets
 * 
 * https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol
 */
public class MultitenantConnectionTest extends AbstractAriesTest {

    @Test
    void testMultitenantWallets() throws Exception {
        
        // Create multitenant wallets
        NessusWallet inviterWallet = createWallet("Faber")
                .ledgerRole(ENDORSER).selfRegisterNym().build();
        
        // Alice does not have a public DID
        NessusWallet inviteeWallet = createWallet("Alice").build();
                
        try {
            
            logSection("Connect Faber to Alice");
            
            ConnectionEventConsumer inviterEvents = new ConnectionEventConsumer();
            inviterEvents.subscribeTo(inviterWallet.getWebSocketEventHandler());
            
            ConnectionEventConsumer inviteeEvents = new ConnectionEventConsumer();
            inviteeEvents.subscribeTo(inviteeWallet.getWebSocketEventHandler());
            
            AriesClient inviter = createClient(inviterWallet);
            AriesClient invitee = createClient(inviteeWallet);
            
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
            
            AssertState.isTrue(inviterEvents.awaitConnectionActive(10, TimeUnit.SECONDS), "NO ACTIVE connections");
            AssertState.isTrue(inviteeEvents.awaitConnectionActive(10, TimeUnit.SECONDS), "NO ACTIVE connections");
            
            String inviterConnectionId = inviterEvents.getConnectionRecord().getConnectionId();
            String inviteeConnectionId = inviteeEvents.getConnectionRecord().getConnectionId();

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
