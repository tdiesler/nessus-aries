
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.util.List;

import org.hyperledger.acy_py.generated.model.InvitationRecord;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.api.out_of_band.CreateInvitationFilter;
import org.hyperledger.aries.api.out_of_band.InvitationCreateRequest;
import org.hyperledger.aries.api.out_of_band.InvitationMessage;
import org.hyperledger.aries.api.out_of_band.ReceiveInvitationFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test RFC 0434: Out-of-Band Protocol 1.1
 * 
 * https://github.com/hyperledger/aries-rfcs/tree/main/features/0434-outofband
 */
public class OutOfBandTest extends AbstractAriesTest {

    @Test
    void testOutOfBand() throws Exception {

        // Create multitenant wallets
        WalletRecord aliceWallet = createWallet("Alice").build();
        WalletRecord faberWallet = createWallet("Faber").role(ENDORSER).build();
        
        try {
            AriesClient alice = useWallet(aliceWallet);
            AriesClient faber = useWallet(faberWallet);

            InvitationRecord invitationRecord = alice.outOfBandCreateInvitation(InvitationCreateRequest.builder()
                    .build(), CreateInvitationFilter.builder().build()).get();
            log.info("Alice: {}", invitationRecord);
            
            List<Object> services = invitationRecord.getInvitation().getServices();
            Assertions.assertEquals(1, services.size(), "Unexpected number of services");
            
            ConnectionRecord connectionRecord = faber.outOfBandReceiveInvitation(InvitationMessage.builder()
                    .service(services.get(0))
                    .build(), ReceiveInvitationFilter.builder().build()).get();
            log.info("Faber: {}", connectionRecord);
            
            assertConnectionState(faberWallet, ConnectionState.INVITATION);
            
        } finally {
            removeWallet(aliceWallet);
            removeWallet(faberWallet);
        }
    }

}
