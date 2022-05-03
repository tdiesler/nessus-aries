
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.TRUSTEE;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;
import org.junit.jupiter.api.Test;

import io.nessus.aries.wallet.NessusWallet;
import io.nessus.aries.wallet.WalletBuilder;

/**
 */
public class OnboardFaberTest extends AbstractAriesTest {

    @Test
    void testOutOfBand() throws Exception {

        // Create initial TRUSTEE Wallet
        NessusWallet governmentWallet = new WalletBuilder("Government")
                .ledgerRole(TRUSTEE).selfRegisterNym().build();

        // Onboard an ENDORSER wallet
        NessusWallet faberWallet = new WalletBuilder("Faber")
                .trusteeWallet(governmentWallet).ledgerRole(ENDORSER).build();
        
        try {
            AriesClient faber = createClient(faberWallet);

            DID did = faber.walletDidPublic().get();
            log.info("Faber: Public {}", did);

        } finally {
            governmentWallet.close();
            faberWallet.close();
        }
    }
}
