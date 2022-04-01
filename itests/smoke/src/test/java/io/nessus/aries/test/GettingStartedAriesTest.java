package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.io.IOException;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.acy_py.generated.model.DIDCreate;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.junit.jupiter.api.Test;

/**
 * The Ledger is externally provided by a running instance of the VON-Network
 * 
 * git clone https://github.com/bcgov/von-network 
 * cd von-network
 * 
 * ./manage build 
 * ./manage up --logs
 * 
 * We run a multi tenant Aries Coudagent
 */
public class GettingStartedAriesTest extends AbstractAriesTest {

    class Context {

        String governmentWalletId;
        String governmentWalletKey;
        String governmentAccessToken;
        DID governmentDid;

        String faberWalletId;
        String faberWalletKey;
        String faberAccessToken;
        DID faberDid;

        String acmeWalletId;
        String acmeWalletKey;
        String acmeAccessToken;
        DID acmeDid;

        String thriftWalletId;
        String thriftWalletKey;
        String thriftAccessToken;
        DID thriftDid;

        String aliceWalletId;
        String aliceWalletKey;
        String aliceAccessToken;
        DID aliceDid;
    }

    @Test
    public void testWorkflow() throws Exception {

        Context ctx = new Context();

        /*
         * Setup Indy Pool Nodes
         * 
         * The ledger is intended to store Identity Records that describe a Ledger
         * Entity.
         * 
         * Identity Records are public data and may include Public Keys, Service
         * Endpoints, Credential Schemas, and Credential Definitions.
         * 
         * Every Identity Record is associated with exactly one DID (Decentralized
         * Identifier) that is globally unique and resolvable (via a ledger) without
         * requiring any centralized resolution authority.
         * 
         * To maintain privacy each Identity Owner can own multiple DIDs.
         */

        // For this example, we use a running instance of the VON-Network
        
        /*
         * Onboard Government Wallet and DID
         * 
         * Trustees operate nodes. Trustees govern the network. These are the highest
         * privileged DIDs. Endorsers are able to write Schemas and Cred_Defs to the
         * ledger, or sign such transactions so they can be written by non-privileged
         * DIDs.
         * 
         * We want to ensure a DID has the least amount of privilege it needs to
         * operate, which in many cases is no privilege, provided the resources it needs
         * are already written to the ledger, either by a privileged DID or by having
         * the txn signed by a privileged DID (e.g. by an Endorser).
         * 
         * An Endorser is a person or organization that the ledger already knows about,
         * that is able to help bootstrap others.
         */

        onboardGovernment(ctx);

        /*
         * Onboarding Faber, Acme, Thrift
         * 
         * Each connection is actually a pair of Pairwise-Unique Identifiers (DIDs). The
         * one DID is owned by one party to the connection and the second by another.
         * 
         * Both parties know both DIDs and understand what connection this pair
         * describes.
         * 
         * Publishing with a DID verification key allows a person, organization or
         * thing, to verify that someone owns this DID as that person, organization or
         * thing is the only one who knows the corresponding signing key and any
         * DID-related operations requiring signing with this key.
         * 
         * The relationship between them is not shareable with others; it is unique to
         * those two parties in that each pairwise relationship uses different DIDs.
         * 
         * We call the process of establish a connection Onboarding.
         */

        onboardFaberColledge(ctx);
        onboardAcmeCorp(ctx);
        onboardThriftBank(ctx);
        onboardAlice(ctx);

        /*
         * Creating Credential Schemas
         * 
         * Credential Schema is the base semantic structure that describes the list of
         * attributes which one particular Credential can contain.
         * 
         * It’s not possible to update an existing Schema. If the Schema needs to be
         * evolved, a new Schema with a new version or name needs to be created.
         * 
         * Schemas in indy are very simple JSON documents that specify their name and
         * version, and that list attributes that will appear in a credential.
         * Currently, they do not describe data type, recurrence rules, nesting, and
         * other elaborate constructs.
         */

//		createTranscriptSchema(ctx);
//		createJobCertificateSchema(ctx);

        /*
         * Creating Credential Definitions
         * 
         * Credential Definition is similar in that the keys that the Issuer uses for
         * the signing of Credentials also satisfies a specific Credential Schema.
         * 
         * It references it's associated schema, announces who is going to be issuing
         * credentials with that schema, what type of signature method they plan to use
         * (“CL” = “Camenisch Lysyanskya”, the default method used for zero-knowledge
         * proofs by indy), how they plan to handle revocation, and so forth.
         * 
         * It’s not possible to update data in an existing Credential Definition. If a
         * CredDef needs to be evolved (for example, a key needs to be rotated), then a
         * new Credential Definition needs to be created by a new Issuer DID.
         * 
         * A Credential Definition can be created and saved in the Ledger an Endorser.
         */

//		createTranscriptCredentialDefinition(ctx);
//		createJobCertificateCredentialDefinition(ctx);

        /*
         * Alice gets her Transcript from Faber College
         * 
         * A credential is a piece of information about an identity — a name, an age, a
         * credit score... It is information claimed to be true. In this case, the
         * credential is named, “Transcript”.
         * 
         * Credentials are offered by an issuer.
         * 
         * An issuer may be any identity owner known to the Ledger and any issuer may
         * issue a credential about any identity owner it can identify.
         * 
         * The usefulness and reliability of a credential are tied to the reputation of
         * the issuer with respect to the credential at hand. For Alice to self-issue a
         * credential that she likes chocolate ice cream may be perfectly reasonable,
         * but for her to self-issue a credential that she graduated from Faber College
         * should not impress anyone.
         */

//		getTranscriptFromFaber(ctx);

        /*
         * Alice applies for a job at Acme
         * 
         * At some time in the future, Alice would like to work for Acme Corp. Normally
         * she would browse to their website, where she would click on a hyperlink to
         * apply for a job. Her browser would download a connection request in which her
         * Indy app would open; this would trigger a prompt to Alice, asking her to
         * accept the connection with Acme Corp.
         *
         * After Alice had established connection with Acme, she got the Job-Application
         * Proof Request. A proof request is a request made by the party who needs
         * verifiable proof of having certain attributes and the solving of predicates
         * that can be provided by other verified credentials.
         * 
         * Acme Corp is requesting that Alice provide a Job Application. The Job
         * Application requires a name, degree, status, SSN and also the satisfaction of
         * the condition about the average mark or grades.
         */

//		applyForJobWithAcme(ctx);

        /*
         * Alice applies for a loan with Thrift Bank
         * 
         * Now that Alice has a job, she’d like to apply for a loan. That will require a
         * proof of employment. She can get this from the Job-Certificate credential
         * offered by Acme.
         */

//		applyForLoanWithThrift(ctx);

        /*
         * Thrift accepts the loan application and now requires KYC
         * 
         * Thrift Bank sends the second Proof Request where Alice needs to share her
         * personal information with the bank.
         */

//		kycProcessWithThrift(ctx);

        /*
         * Alice decides to quit her job with Acme
         */

//		quitJobWithAcme(ctx);

        // Close and Delete the Wallets

        closeAndDeleteWallets(ctx);
    }

    private void onboardGovernment(Context ctx) throws IOException {

        String walletKey = "govwkey";
        WalletRecord walletRecord = createWallet("Government", walletKey);
        String walletName = walletRecord.getSettings().getWalletName();
        log.info("Wallet: {}", walletRecord);

        // Create client for sub wallet
        AriesClient client = createWalletClient(walletRecord.getToken());

        // Create a local DID
        //
        // [#1682] Allow use of SEED when creating local wallet DID
        // https://github.com/hyperledger/aries-cloudagent-python/issues/1682
        DID didResponse = client.walletDidCreate(DIDCreate.builder().build()).get();
        log.info("{} DID: {}", walletName, didResponse);

        // A wallet can normally not self-register a newly created DID
        // This has to go through an authorized Agent
        // Here we cheat a little and use the VON-Network Admin page to register the DID
        // [TODO] Use an external endorser agent
        selfRegisterDid(didResponse.getDid(), didResponse.getVerkey(), ENDORSER);

        // Set the public DID for the wallet
        client.walletDidPublic(didResponse.getDid());

        ctx.governmentWalletId = walletRecord.getWalletId();
        ctx.governmentAccessToken = walletRecord.getToken();
        ctx.governmentWalletKey = walletKey;
        ctx.governmentDid = didResponse;
    }

    private void onboardFaberColledge(Context ctx) throws IOException {

        String walletKey = "fabwkey";
        WalletRecord walletRecord = createWallet("Faber", walletKey);
        String walletName = walletRecord.getSettings().getWalletName();
        log.info("Wallet: {}", walletRecord);

        // Create client for sub wallet
        AriesClient client = createWalletClient(walletRecord.getToken());

        // Create a local DID
        //
        // [#1682] Allow use of SEED when creating local wallet DID
        // https://github.com/hyperledger/aries-cloudagent-python/issues/1682
        DID didResponse = client.walletDidCreate(DIDCreate.builder().build()).get();
        log.info("{} DID: {}", walletName, didResponse);

        // A wallet can normally not self-register a newly created DID
        // This has to go through an authorized Agent
        // Here we cheat a little and use the VON-Network Admin page to register the DID
        // [TODO] Use an external endorser agent
        selfRegisterDid(didResponse.getDid(), didResponse.getVerkey(), ENDORSER);

        // Set the public DID for the wallet
        client.walletDidPublic(didResponse.getDid());

        ctx.faberWalletId = walletRecord.getWalletId();
        ctx.faberAccessToken = walletRecord.getToken();
        ctx.faberWalletKey = walletKey;
        ctx.faberDid = didResponse;
    }

    private void onboardAcmeCorp(Context ctx) throws IOException {

        String walletKey = "acmewkey";
        WalletRecord walletRecord = createWallet("Acme", walletKey);
        String walletName = walletRecord.getSettings().getWalletName();
        log.info("Wallet: {}", walletRecord);

        // Create client for sub wallet
        AriesClient client = createWalletClient(walletRecord.getToken());

        // Create a local DID
        //
        // [#1682] Allow use of SEED when creating local wallet DID
        // https://github.com/hyperledger/aries-cloudagent-python/issues/1682
        DID didResponse = client.walletDidCreate(DIDCreate.builder().build()).get();
        log.info("{} DID: {}", walletName, didResponse);

        // A wallet can normally not self-register a newly created DID
        // This has to go through an authorized Agent
        // Here we cheat a little and use the VON-Network Admin page to register the DID
        // [TODO] Use an external endorser agent
        selfRegisterDid(didResponse.getDid(), didResponse.getVerkey(), ENDORSER);

        // Set the public DID for the wallet
        client.walletDidPublic(didResponse.getDid());

        ctx.acmeWalletId = walletRecord.getWalletId();
        ctx.acmeAccessToken = walletRecord.getToken();
        ctx.acmeWalletKey = walletKey;
        ctx.acmeDid = didResponse;
    }

    private void onboardThriftBank(Context ctx) throws IOException {

        String walletKey = "thriftwkey";
        WalletRecord walletRecord = createWallet("Thrift", walletKey);
        String walletName = walletRecord.getSettings().getWalletName();
        log.info("Wallet: {}", walletRecord);

        // Create client for sub wallet
        AriesClient client = createWalletClient(walletRecord.getToken());

        // Create a local DID
        //
        // [#1682] Allow use of SEED when creating local wallet DID
        // https://github.com/hyperledger/aries-cloudagent-python/issues/1682
        DID didResponse = client.walletDidCreate(DIDCreate.builder().build()).get();
        log.info("{} DID: {}", walletName, didResponse);

        // A wallet can normally not self-register a newly created DID
        // This has to go through an authorized Agent
        // Here we cheat a little and use the VON-Network Admin page to register the DID
        // [TODO] Use an external endorser agent
        selfRegisterDid(didResponse.getDid(), didResponse.getVerkey(), ENDORSER);

        // Set the public DID for the wallet
        client.walletDidPublic(didResponse.getDid());

        ctx.thriftWalletId = walletRecord.getWalletId();
        ctx.thriftAccessToken = walletRecord.getToken();
        ctx.thriftWalletKey = walletKey;
        ctx.thriftDid = didResponse;
    }

    private void onboardAlice(Context ctx) throws IOException {

        String walletKey = "alicewkey";
        WalletRecord walletRecord = createWallet("Alice", walletKey);
        String walletName = walletRecord.getSettings().getWalletName();
        log.info("Wallet: {}", walletRecord);

        // Create client for sub wallet
        AriesClient client = createWalletClient(walletRecord.getToken());

        // Create a local DID
        //
        // [#1682] Allow use of SEED when creating local wallet DID
        // https://github.com/hyperledger/aries-cloudagent-python/issues/1682
        DID didResponse = client.walletDidCreate(DIDCreate.builder().build()).get();
        log.info("{} DID: {}", walletName, didResponse);

        ctx.aliceWalletId = walletRecord.getWalletId();
        ctx.aliceAccessToken = walletRecord.getToken();
        ctx.aliceWalletKey = walletKey;
        ctx.aliceDid = didResponse;
    }

    private void closeAndDeleteWallets(Context ctx) {
        removeWallet(ctx.governmentWalletId, ctx.governmentWalletKey);
        removeWallet(ctx.faberWalletId, ctx.faberWalletKey);
        removeWallet(ctx.acmeWalletId, ctx.acmeWalletKey);
        removeWallet(ctx.thriftWalletId, ctx.thriftWalletKey);
        removeWallet(ctx.aliceWalletId, ctx.aliceWalletKey);
    }
}
