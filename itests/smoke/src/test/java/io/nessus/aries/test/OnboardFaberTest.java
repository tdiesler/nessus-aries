
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.TRUSTEE;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.junit.jupiter.api.Test;

/**
 */
public class OnboardFaberTest extends AbstractAriesTest {

    @Test
    void testOutOfBand() throws Exception {

        // Create initial TRUSTEE Wallet
        WalletRecord govrnWallet = createWallet("Government", TRUSTEE);
        
        // Onboard an ENDORSER wallet
        WalletRecord faberWallet = createWallet(govrnWallet, "Faber", ENDORSER);
        
        try {
            AriesClient faber = useWallet(faberWallet);

            DID did = faber.walletDidPublic().get();
            log.info("Faber: Public {}", did);

        } finally {
            removeWallet(faberWallet);
            removeWallet(govrnWallet);
        }
    }
}
