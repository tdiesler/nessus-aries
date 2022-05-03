
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.acy_py.generated.model.GetNymRoleResponse;
import org.hyperledger.acy_py.generated.model.GetNymRoleResponse.RoleEnum;
import org.hyperledger.aries.AriesClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nessus.aries.wallet.NessusWallet;
import io.nessus.aries.wallet.WalletBuilder;

/**
 * Test the Acapy wallet endpoint
 */
public class NessusWalletTest extends AbstractAriesTest {

    @Test
    void testMultitenantWallet() throws Exception {

        // Create multitenant wallet
        NessusWallet faberWallet = new WalletBuilder("Faber")
                .ledgerRole(ENDORSER).selfRegisterNym().build();
        try {
            
            AriesClient client = createClient(faberWallet);
            DID did = client.walletDidPublic().get();
            
            // Verify that we can access the ledger
            GetNymRoleResponse nymRoleResponse = client.ledgerGetNymRole(did.getDid()).get();
            log.info("{}: {}", faberWallet.getWalletName(), nymRoleResponse);
            
            // [TODO] [#19] Multiple variants of role enum
            Assertions.assertEquals(RoleEnum.ENDORSER, nymRoleResponse.getRole());
            
        } finally {
            faberWallet.closeAndRemove();
        }
    }
}
