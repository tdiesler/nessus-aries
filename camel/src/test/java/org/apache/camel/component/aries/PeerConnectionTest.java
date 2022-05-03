package org.apache.camel.component.aries;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aries.connection.ConnectionEventProcessor;
import org.apache.camel.component.aries.connection.ConnectionResultProcessor;
import org.apache.camel.component.aries.connection.ConnectionResultProcessor.ConnectionResult;
import org.hyperledger.aries.api.connection.ConnectionReceiveInvitationFilter;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.connection.CreateInvitationRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * docker compose up --detach && docker compose logs -f acapy
 */
public class PeerConnectionTest extends AbstractHyperledgerAriesTest {

    static final String Faber = "Faber";
    static final String Alice = "Alice";
    
    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                
                from("direct:faber-alice-connect")
                
                    // Faber creates the connection Invitation
                    .to("hyperledger-aries:faber?service=/connections/create-invitation")
                    
                    // Transform CreateInvitationResponse => ConnectionInvitation
                    .transform(simple("${body.invitation}"))
                    
                    // Set an additional message header for Inivitation auto accept
                    // We could also have done this when initiating the route
                    .setHeader(ConnectionReceiveInvitationFilter.class.getSimpleName(), () ->
                            ConnectionReceiveInvitationFilter.builder().autoAccept(true).build())
                    
                    // Setup WebSocket event handling for the Inviter/Invitee
                    .process(new ConnectionEventProcessor(Faber))
                    .process(new ConnectionEventProcessor(Alice))
                    
                    // Alice consumes the Invitation
                    .to("hyperledger-aries:alice?service=/connections/receive-invitation")
                    
                    // Await connection ACTIVE for the Inviter/Invitee
                    .process(new ConnectionResultProcessor(Faber, Alice));
            }
        };
    }

    @Test
    public void testWorkflow() throws Exception {
        
        getComponent().setRemoveWalletsOnShutdown(true);
        
        onboardWallet(Faber, ENDORSER);
        onboardWallet(Alice);
        
        CreateInvitationRequest createInvitation = CreateInvitationRequest.builder().build();
        
        ConnectionResult resObj = template.requestBody("direct:faber-alice-connect", createInvitation, ConnectionResult.class);
        
        log.info("Inviter: [{}] {}", resObj.inviterConnection.getState(), resObj.inviterConnection);
        log.info("Invitee: [{}] {}", resObj.inviteeConnection.getState(), resObj.inviteeConnection);
        
        Assertions.assertEquals(ConnectionState.ACTIVE, resObj.inviterConnection.getState());
        Assertions.assertEquals(ConnectionState.ACTIVE, resObj.inviteeConnection.getState());
    }
}
