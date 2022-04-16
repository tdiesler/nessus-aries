
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nessus.aries.common.ConnectionManger;
import io.nessus.aries.common.ConnectionManger.ConnectionResult;

/**
 * Test RFC 0160: Connection Protocol with multitenant wallets
 * 
 * https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol
 */
public class MultitenantConnectionTest extends AbstractAriesTest {

    @Test
    void testMultitenantWallets() throws Exception {
        
        // Create multitenant wallets
        WalletRecord faberWallet = new WalletBuilder("Faber")
                .ledgerRole(ENDORSER).selfRegisterNym().build();
        
        // Alice does not have a public DID
        WalletRecord aliceWallet = new WalletBuilder("Alice").build();
                
        log.info("===================================================================================");
        
        try {
            
            ConnectionResult connectResult = ConnectionManger.connect(faberWallet, aliceWallet).get();
            log.info("{}", connectResult);
            
            Assertions.assertTrue(connectResult.isActive(), "Connections not active: " + connectResult);
            
        } finally {
            removeWallet(faberWallet);
            removeWallet(aliceWallet);
        }
    }
}
