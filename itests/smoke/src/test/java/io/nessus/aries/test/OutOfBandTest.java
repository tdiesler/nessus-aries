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

import org.hyperledger.acy_py.generated.model.InvitationRecord;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.out_of_band.CreateInvitationFilter;
import org.hyperledger.aries.api.out_of_band.InvitationCreateRequest;
import org.hyperledger.aries.api.out_of_band.InvitationMessage;
import org.hyperledger.aries.api.out_of_band.ReceiveInvitationFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nessus.aries.wallet.NessusWallet;
import io.nessus.aries.wallet.WalletBuilder;

/**
 * Test RFC 0434: Out-of-Band Protocol 1.1
 * 
 * https://github.com/hyperledger/aries-rfcs/tree/main/features/0434-outofband
 */
public class OutOfBandTest extends AbstractAriesTest {

    @Test
    void testOutOfBand() throws Exception {

        // Create multitenant wallets
        NessusWallet faberWallet = new WalletBuilder("Faber")
                .ledgerRole(ENDORSER).selfRegisterNym().build();
        
        NessusWallet aliceWallet = new WalletBuilder("Alice").build();
        
        try {
            AriesClient alice = createClient(aliceWallet);
            AriesClient faber = createClient(faberWallet);

            InvitationRecord invitationRecord = alice.outOfBandCreateInvitation(InvitationCreateRequest.builder()
                    .build(), CreateInvitationFilter.builder().build()).get();
            log.info("Alice: {}", invitationRecord);
            
            List<Object> services = invitationRecord.getInvitation().getServices();
            Assertions.assertEquals(1, services.size(), "Unexpected number of services");
            
            ConnectionRecord connectionRecord = faber.outOfBandReceiveInvitation(InvitationMessage.builder()
                    .service(services.get(0))
                    .build(), ReceiveInvitationFilter.builder().build()).get();
            log.info("Faber: {}", connectionRecord);
            
            //assertConnectionState(faberWallet, ConnectionState.INVITATION);
            
        } finally {
            aliceWallet.closeAndRemove();
            faberWallet.closeAndRemove();
        }
    }

}
