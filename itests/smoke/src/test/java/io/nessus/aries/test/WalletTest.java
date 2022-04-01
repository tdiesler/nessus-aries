
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.acy_py.generated.model.DIDCreate;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test the Acapy wallet endpoint
 */
public class WalletTest extends AbstractAriesTest {

    @Test
    void testMultitenantWallet() throws Exception {

        // Create multitenant wallet
        String walletKey = "alicewkey";
        WalletRecord walletRecord = createWallet("alice", walletKey);
        log.info("Wallet: {}", walletRecord);

        // Get wallet by wallet id
        WalletRecord walletRecordReloaded = getWallet(walletRecord.getWalletId());
        Assertions.assertEquals(walletRecord.toString(), walletRecordReloaded.toString());

        // Create client for sub wallet
        AriesClient alice = createWalletClient(walletRecord.getToken());

        // Create local did
        //
        // [#1682] Allow use of SEED when creating local wallet DID
        // https://github.com/hyperledger/aries-cloudagent-python/issues/1682
        DID aliceDid = alice.walletDidCreate(DIDCreate.builder().build()).get();
        log.info("Alice DID: {}", aliceDid);

        selfRegisterDid(aliceDid.getDid(), aliceDid.getVerkey(), ENDORSER);

        // Set the public DID for the wallet
        alice.walletDidPublic(aliceDid.getDid());

        // Delete wallet
        removeWallet(walletRecord.getWalletId(), walletKey);
    }
}
