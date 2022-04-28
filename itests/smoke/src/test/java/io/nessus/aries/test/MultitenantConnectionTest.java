
package io.nessus.aries.test;

import static io.nessus.aries.wallet.ConnectionHelper.connectPeers;
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.util.List;
import java.util.Map;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test RFC 0160: Connection Protocol with multitenant wallets
 * 
 * https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol
 */
public class MultitenantConnectionTest extends AbstractAriesTest {

    @Test
    void testMultitenantWallets() throws Exception {
        
        // Create multitenant wallets
        WalletRecord inviterWallet = new WalletBuilder("Faber")
                .ledgerRole(ENDORSER).selfRegisterNym().build();
        
        // Alice does not have a public DID
        WalletRecord inviteeWallet = new WalletBuilder("Alice").build();
                
        try {
            
            logSection("Connect Faber to Alice");
            
            Map<String, ConnectionRecord> connections = connectPeers(inviterWallet, inviteeWallet);
            
            ConnectionRecord inviterConnection = connections.get(inviterWallet.getWalletId());
            String inviterConnectionId = inviterConnection.getConnectionId();

            ConnectionRecord inviteeConnection = connections.get(inviteeWallet.getWalletId());
            String inviteeConnectionId = inviteeConnection.getConnectionId();
            
            AriesClient inviter = createClient(inviterWallet);
            AriesClient invitee = createClient(inviteeWallet);
            
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
            removeWallet(inviteeWallet);
            removeWallet(inviterWallet);
        }
    }
}
