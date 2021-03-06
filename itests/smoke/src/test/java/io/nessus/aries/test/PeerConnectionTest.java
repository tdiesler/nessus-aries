/*-
 * #%L
 * Nessus Aries :: Tests :: Smoke
 * %%
 * Copyright (C) 2022 Nessus
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
import org.hyperledger.aries.webhook.EventType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nessus.aries.wallet.NessusWallet;
import io.nessus.aries.websocket.WebSocketClient;

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

            AriesClient inviter = inviterWallet.createClient();
            AriesClient invitee = inviteeWallet.createClient();
            
            WebSocketClient inviterWSClient = inviterWallet.createWebSocketClient().startRecording(EventType.CONNECTIONS);            
            WebSocketClient inviteeWSClient = inviteeWallet.createWebSocketClient().startRecording(EventType.CONNECTIONS);            
            
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

            ConnectionRecord inviterConnectionRecord = inviterWSClient
            		.awaitConnection(ConnectionRecord::stateIsActive, 10, TimeUnit.SECONDS)
                    .findAny().get();
            
            ConnectionRecord inviteeConnectionRecord = inviteeWSClient
            		.awaitConnection(ConnectionRecord::stateIsActive, 10, TimeUnit.SECONDS)
                    .findAny().get();
            
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
