
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.acy_py.generated.model.GetNymRoleResponse;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.junit.jupiter.api.Test;

/**
 * Test the Acapy wallet endpoint
 */
public class WalletTest extends AbstractAriesTest {

    @Test
    void testMultitenantWallet() throws Exception {

        // Create multitenant wallet
        WalletRecord wallet = createWallet("Faber").role(ENDORSER).build();
        String walletName = wallet.getSettings().getWalletName();
        
        AriesClient client = useWallet(wallet);
        DID did = client.walletDidPublic().get();
        
        // Verify that we can access the ledger
        GetNymRoleResponse nymRoleResponse = client.ledgerGetNymRole(did.getDid()).get();
        log.info("{}: {}", walletName, nymRoleResponse);
        
        // Delete wallet
        removeWallet(wallet);
    }
}
