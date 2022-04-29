
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.TRUSTEE;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.junit.jupiter.api.Test;

import io.nessus.aries.wallet.WalletBuilder;

/**
 */
public class OnboardFaberTest extends AbstractAriesTest {

    @Test
    void testOutOfBand() throws Exception {

        // Create initial TRUSTEE Wallet
        WalletRecord govrnWallet = new WalletBuilder("Government")
                .ledgerRole(TRUSTEE).selfRegisterNym().build();

        // Onboard an ENDORSER wallet
        WalletRecord faberWallet = new WalletBuilder("Faber")
                .registerNym(govrnWallet).ledgerRole(ENDORSER).build();
        
        try {
            AriesClient faber = createClient(faberWallet);

            DID did = faber.walletDidPublic().get();
            log.info("Faber: Public {}", did);

        } finally {
            removeWallet(faberWallet);
            removeWallet(govrnWallet);
        }
    }
}
