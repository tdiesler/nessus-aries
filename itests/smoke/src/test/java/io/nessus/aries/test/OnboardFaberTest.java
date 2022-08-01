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
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.TRUSTEE;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;
import org.junit.jupiter.api.Test;

import io.nessus.aries.wallet.NessusWallet;
import io.nessus.aries.wallet.WalletBuilder;
public class OnboardFaberTest extends AbstractAriesTest {

    @Test
    void testOutOfBand() throws Exception {

        // Create initial TRUSTEE Wallet
        NessusWallet governmentWallet = new WalletBuilder("Government")
                .ledgerRole(TRUSTEE)
                .selfRegisterNym()
                .build();

        // Onboard an ENDORSER wallet
        NessusWallet faberWallet = new WalletBuilder("Faber")
                .trusteeWallet(governmentWallet)
                .ledgerRole(ENDORSER)
                .build();
        
        try {
            AriesClient faber = faberWallet.createClient();

            DID did = faber.walletDidPublic().get();
            log.info("Faber: Public {}", did);

        } finally {
            faberWallet.closeAndRemove();
            governmentWallet.closeAndRemove();
        }
    }
}
