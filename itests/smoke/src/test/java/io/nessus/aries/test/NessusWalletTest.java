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
