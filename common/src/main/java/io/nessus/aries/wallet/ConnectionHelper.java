package io.nessus.aries.wallet;

import java.util.HashMap;
import java.util.Map;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.aries.Configuration;
import io.nessus.aries.coms.WebSocketEventHandler;
import io.nessus.aries.coms.WebSocketEventHandler.WebSocketEvent;
import io.nessus.aries.coms.WebSockets;
import io.nessus.aries.util.AssertState;
import io.nessus.aries.util.SafeConsumer;
import okhttp3.WebSocket;

public class ConnectionHelper {
    
    static final Logger log = LoggerFactory.getLogger(ConnectionHelper.class);
    
    // Hide ctor
    private ConnectionHelper() {
    }
    
    public static Map<String, ConnectionRecord> connectPeers(WalletRecord inviterWallet, WalletRecord inviteeWallet) throws Exception {
        
        Map<String, ConnectionRecord> connections = new HashMap<>();
        CountDownLatch peerConnectionLatch = new CountDownLatch(2);
        
        SafeConsumer<WebSocketEvent> eventConsumer = ev -> {
            String thisName = ev.getThisWalletName();
            String theirName = ev.getTheirWalletName();
            ConnectionRecord con = ev.getPayload(ConnectionRecord.class);
            log.info("{}: [@{}] {} {} {}", thisName, theirName, con.getTheirRole(), con.getState(), con);
            connections.put(ev.getThisWalletId(), con);
            if (ConnectionState.ACTIVE == con.getState()) {
                peerConnectionLatch.countDown();
            }
        };
        
        WebSocket inviterSocket = WebSockets.createWebSocket(inviterWallet, new WebSocketEventHandler.Builder()
                .subscribe(inviterWallet.getWalletId(), ConnectionRecord.class, eventConsumer)
                .walletRegistry(new WalletRegistry(inviterWallet, inviteeWallet))
                .build());
        
        WebSocket inviteeSocket = WebSockets.createWebSocket(inviteeWallet, new WebSocketEventHandler.Builder()
                .subscribe(inviteeWallet.getWalletId(), ConnectionRecord.class, eventConsumer)
                .walletRegistry(new WalletRegistry(inviterWallet, inviteeWallet))
                .build());
        
        AriesClient inviter = Configuration.createClient(inviterWallet);
        AriesClient invitee = Configuration.createClient(inviteeWallet);
        
        // Invitee creates an invitation (/connections/create-invitation)
        CreateInvitationResponse response = invitee.connectionsCreateInvitation(CreateInvitationRequest.builder().build()).get();
        ConnectionInvitation invitation = response.getInvitation();
        
        // Inviter receives the invitation from the Invitee (/connections/receive-invitation)
        inviter.connectionsReceiveInvitation(ReceiveInvitationRequest.builder()
                .recipientKeys(invitation.getRecipientKeys())
                .serviceEndpoint(invitation.getServiceEndpoint())
                .build(), ConnectionReceiveInvitationFilter.builder()
                    .autoAccept(true)
                    .build()).get();

        AssertState.isTrue(peerConnectionLatch.await(10, TimeUnit.SECONDS), "NO ACTIVE connections");
        
        WebSockets.closeWebSocket(inviterSocket);
        WebSockets.closeWebSocket(inviteeSocket);
        
        return connections;
    }
}