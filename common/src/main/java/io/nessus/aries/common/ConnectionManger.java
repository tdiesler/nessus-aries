package io.nessus.aries.common;

import static io.nessus.aries.common.Configuration.createClient;
import static io.nessus.aries.common.WebSockets.closeWebSocket;
import static io.nessus.aries.common.WebSockets.createWebSocket;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.WebSocket;

/**
 * RFC 0160: Connection Protocol with multitenant wallets
 * 
 * Requires --auto-ping-connection otherwise Inviter gets stuck in state RESPONSE
 * 
 * https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol
 */
public final class ConnectionManger {

    static final Logger log = LoggerFactory.getLogger(ConnectionManger.class);

    // Hide ctor
    private ConnectionManger() {}
    
    public static class ConnectionResult {
        public final WalletRecord inviterWallet;
        public final WalletRecord inviteeWallet;
        private final Map<String, ConnectionRecord> connections = new HashMap<>();
        
        ConnectionResult(WalletRecord inviterWallet, WalletRecord inviteeWallet) {
            this.inviterWallet = inviterWallet;
            this.inviteeWallet = inviteeWallet;
            connections.put(inviterWallet.getWalletId(), new ConnectionRecord());
            connections.put(inviteeWallet.getWalletId(), new ConnectionRecord());
        }

        public ConnectionRecord getInviterConnection() {
            return connections.get(inviterWallet.getWalletId());
        }
        
        public ConnectionRecord getInviteeConnection() {
            return connections.get(inviteeWallet.getWalletId());
        }
        
        public boolean isActive() {
            ConnectionRecord conA = getInviterConnection();
            ConnectionRecord conB = getInviteeConnection();
            return conA != null && conB != null && conA.getState() == conB.getState();
        }
        
        @Override
        public String toString() {
            String inviterName = inviterWallet.getSettings().getWalletName();
            String inviteeName = inviteeWallet.getSettings().getWalletName();
            ConnectionRecord conA = getInviterConnection();
            ConnectionRecord conB = getInviteeConnection();
            return String.format("ConnectionResult [Inviter: %s %s %s, Invitee: %s %s %s]", 
                    inviterName, conA.getConnectionId(), conA.getState(), 
                    inviteeName, conB.getConnectionId(), conB.getState());
        }
    }

    public static Optional<ConnectionResult> connect(WalletRecord inviterWallet, WalletRecord inviteeWallet) throws Exception {
        return connect(inviterWallet, inviteeWallet, 10, TimeUnit.SECONDS);
    }

    public static Optional<ConnectionResult> connect(WalletRecord inviterWallet, WalletRecord inviteeWallet, long timeout, TimeUnit unit) throws Exception {
        
        AriesClient inviter = createClient(inviterWallet);
        AriesClient invitee = createClient(inviteeWallet);
        
        ConnectionResult result = new ConnectionResult(inviterWallet, inviteeWallet);
        
        CountDownLatch activeLatch = new CountDownLatch(2);
        class WSEHandler extends WebSocketEventHandler  {
            
            @Override
            public void handleConnection(String walletId, ConnectionRecord con) throws Exception {
                synchronized (result) {
                    super.handleConnection(walletId, con);
                    if (myWalletId.equals(walletId)) {
                        result.connections.put(walletId, con);
                        if (ConnectionState.ACTIVE == con.getState()) {
                            log.info("{} CONNECTION ACTIVE", myWalletName);
                            activeLatch.countDown();
                        }
                    }
                }
            }
        };
        
        WebSocket inviterSocket = createWebSocket(inviterWallet, new WSEHandler());
        WebSocket inviteeSocket = createWebSocket(inviteeWallet, new WSEHandler());
        
        try {
            // Inviter creates an invitation (/connections/create-invitation)
            CreateInvitationResponse response = inviter.connectionsCreateInvitation(
                    CreateInvitationRequest.builder().build(), 
                    CreateInvitationParams.builder()
                        .autoAccept(true)
                        .build()).get();
            String inviterName = inviterWallet.getSettings().getWalletName();
            ConnectionInvitation invitation = response.getInvitation();
            log.info("{}: {}", inviterName, invitation);
            
            // Invitee receives the invitation from the Inviter (/connections/receive-invitation)
            invitee.connectionsReceiveInvitation(ReceiveInvitationRequest.builder()
                    .recipientKeys(invitation.getRecipientKeys())
                    .serviceEndpoint(invitation.getServiceEndpoint())
                    .build(), ConnectionReceiveInvitationFilter.builder()
                        .autoAccept(true)
                        .build()).get();
            
            Optional<ConnectionResult> optional = Optional.of(result);
            activeLatch.await(timeout, unit);
            
            return optional;
            
        } finally {
            closeWebSocket(inviterSocket);
            closeWebSocket(inviteeSocket);
        }
    }
}