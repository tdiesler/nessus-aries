package io.nessus.aries.wallet;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hyperledger.acy_py.generated.model.ConnectionInvitation;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionReceiveInvitationFilter;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.connection.ConnectionTheirRole;
import org.hyperledger.aries.api.connection.CreateInvitationRequest;
import org.hyperledger.aries.api.connection.CreateInvitationResponse;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.aries.AriesClientFactory;
import io.nessus.aries.coms.EventSubscriber;
import io.nessus.aries.coms.WebSocketEventHandler.WebSocketEvent;
import io.nessus.aries.util.AssertState;
import io.nessus.aries.util.SafeConsumer;

/**
 * RFC 0160: Connection Protocol with multitenant wallets
 * 
 * Requires --auto-ping-connection otherwise Inviter gets stuck in state RESPONSE
 * 
 * https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol
 */
public class ConnectionHelper {
    
    static final Logger log = LoggerFactory.getLogger(ConnectionHelper.class);
    
    // Hide ctor
    private ConnectionHelper() {
    }
    
    public static class ConnectionResult {
        private ConnectionRecord inviterConnection;
        private ConnectionRecord inviteeConnection;
        
        public ConnectionRecord getInviterConnection() {
            return inviterConnection;
        }
        
        public ConnectionRecord getInviteeConnection() {
            return inviteeConnection;
        }
    }
    
    public static ConnectionResult connectPeers(NessusWallet inviterWallet, NessusWallet inviteeWallet) throws Exception {
        
        ConnectionResult connectionResult = new ConnectionResult();
        CountDownLatch peerConnectionLatch = new CountDownLatch(2);
        
        SafeConsumer<WebSocketEvent> eventConsumer = ev -> {
            String thisName = ev.getThisWalletName();
            String theirName = ev.getTheirWalletName();
            ConnectionRecord con = ev.getPayload(ConnectionRecord.class);
            log.info("{}: [@{}] {} {} {}", thisName, theirName, con.getTheirRole(), con.getState(), con);
            if (con.getTheirRole() == ConnectionTheirRole.INVITEE) 
                connectionResult.inviterConnection = con;
            if (con.getTheirRole() == ConnectionTheirRole.INVITER) 
                connectionResult.inviteeConnection = con;
            if (ConnectionState.ACTIVE == con.getState()) {
                peerConnectionLatch.countDown();
            }
        };
        
        EventSubscriber<WebSocketEvent> inviterSubscriber = inviterWallet.getWebSocketEventHandler()
                .subscribe(ConnectionRecord.class, eventConsumer);
        
        EventSubscriber<WebSocketEvent> inviteeSubscriber = inviteeWallet.getWebSocketEventHandler()
                .subscribe(ConnectionRecord.class, eventConsumer);
        
        AriesClient inviter = AriesClientFactory.createClient(inviterWallet);
        AriesClient invitee = AriesClientFactory.createClient(inviteeWallet);
        
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

        AssertState.isTrue(peerConnectionLatch.await(10, TimeUnit.SECONDS), "NO ACTIVE connections");
        
        inviterSubscriber.cancelSubscription();
        inviteeSubscriber.cancelSubscription();
        
        return connectionResult;
    }
}