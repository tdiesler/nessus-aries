package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionStaticRequest;
import org.hyperledger.aries.api.connection.ConnectionStaticResult;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionResponse;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialProposalRequest;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.api.revocation.RevRegCreateRequest;
import org.hyperledger.aries.api.revocation.RevRegCreateResponse;
import org.hyperledger.aries.api.schema.SchemaSendRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.aries.api.schema.SchemaSendResponse.Schema;
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

        DID governmentDid;
        WalletRecord governmentWallet;

        DID faberDid;
        WalletRecord faberWallet;
        String faberTranscriptCredDefId;

        DID acmeDid;
        WalletRecord acmeWallet;
        String acmeJobCertificateCredDefId;
        String acmeJobCertificateRevocationRegistryId;

        DID thriftDid;
        WalletRecord thriftWallet;

        DID aliceDid;
        WalletRecord aliceWallet;
        String faberAliceConnectionId;

        String transcriptSchemaId;
        String jobCertificateSchemaId;
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
//        onboardThriftBank(ctx);
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

        createTranscriptSchema(ctx);
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

        createTranscriptCredentialDefinition(ctx);
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

        try {
            getTranscriptFromFaber(ctx);
        } catch (Exception ex) {
            closeAndDeleteWallets(ctx);
            throw ex;
        }

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

        WalletRecord wallet = createWallet("Government").role(ENDORSER).build();

        // Create client for sub wallet
        AriesClient client = useWallet(wallet);
        DID publicDid = client.walletDidPublic().get();

        ctx.governmentWallet = wallet;
        ctx.governmentDid = publicDid;
    }

    private void onboardFaberColledge(Context ctx) throws IOException {

        WalletRecord wallet = createWallet("Faber").role(ENDORSER).build();

        // Create client for sub wallet
        AriesClient client = useWallet(wallet);
        DID publicDid = client.walletDidPublic().get();

        ctx.faberWallet = wallet;
        ctx.faberDid = publicDid;
    }

    private void onboardAcmeCorp(Context ctx) throws IOException {

        WalletRecord wallet = createWallet("Acme").role(ENDORSER).build();

        // Create client for sub wallet
        AriesClient client = useWallet(wallet);
        DID publicDid = client.walletDidPublic().get();

        ctx.acmeWallet = wallet;
        ctx.acmeDid = publicDid;
    }

    private void onboardThriftBank(Context ctx) throws IOException {

        WalletRecord wallet = createWallet("Thrift").role(ENDORSER).build();

        // Create client for sub wallet
        AriesClient client = useWallet(wallet);
        DID publicDid = client.walletDidPublic().get();

        ctx.thriftWallet = wallet;
        ctx.thriftDid = publicDid;
    }

    private void onboardAlice(Context ctx) throws IOException {

        WalletRecord wallet = createWallet("Alice").build();

        // Create client for sub wallet
        AriesClient client = useWallet(wallet);
        DID publicDid = client.walletDidPublic().get();

        ctx.aliceWallet = wallet;
        ctx.aliceDid = publicDid;
    }

    private void createTranscriptSchema(Context ctx) throws IOException {

        // Government creates the Transcript Credential Schema and sends it to the Ledger
        // It can do so with it's Endorser role

        // Create client for sub wallet
        AriesClient government = useWallet(ctx.governmentWallet);

        SchemaSendResponse schemaResponse = government.schemas(SchemaSendRequest.builder()
                .schemaVersion("1.2")
                .schemaName("Transcript")
                .attributes(Arrays.asList("first_name", "last_name", "degree", "status", "year", "average", "ssn"))
                .build()).get();

        ctx.transcriptSchemaId = schemaResponse.getSchemaId();
    }

    void createJobCertificateSchema(Context ctx) throws Exception {

        // Government creates the Job-Certificate Credential Schema and sends it to the Ledger
        // It can do so with it's Endorser role

        // Create client for sub wallet
        AriesClient government = useWallet(ctx.governmentWallet);

        SchemaSendResponse schemaResponse = government.schemas(SchemaSendRequest.builder()
                .schemaVersion("0.2")
                .schemaName("Job-Certificate")
                .attributes(Arrays.asList("first_name", "last_name", "salary", "employee_status", "experience"))
                .build()).get();

        ctx.jobCertificateSchemaId = schemaResponse.getSchemaId();
    }

    void createTranscriptCredentialDefinition(Context ctx) throws Exception {

        // 1. Faber get the Transcript Credential Schema

        // Create client for sub wallet
        AriesClient faber = useWallet(ctx.faberWallet);

        Schema schema = faber.schemasGetById(ctx.transcriptSchemaId).get();
        log.info("{}", schema);

        // 2. Faber creates the Credential Definition related to the received Credential Schema and send it to the ledger

        CredentialDefinitionResponse creddefResponse = faber.credentialDefinitionsCreate(CredentialDefinitionRequest.builder()
                .schemaId(schema.getId())
                .supportRevocation(false)
                .build()).get();
        log.info("{}", creddefResponse);

        ctx.faberTranscriptCredDefId = creddefResponse.getCredentialDefinitionId();
    }

    void createJobCertificateCredentialDefinition(Context ctx) throws Exception {

        // 1. Acme get the Transcript Credential Schema

        // Create client for sub wallet
        AriesClient acme = useWallet(ctx.acmeWallet);

        Schema schema = acme.schemasGetById(ctx.jobCertificateSchemaId).get();
        log.info("{}", schema);

        // 2. Acme creates the Credential Definition related to the received Credential Schema and send it to the ledger

        CredentialDefinitionResponse creddefResponse = acme.credentialDefinitionsCreate(CredentialDefinitionRequest.builder()
                .schemaId(schema.getId())
                .supportRevocation(true)
                .build()).get();
        log.info("{}", creddefResponse);

        ctx.acmeJobCertificateCredDefId = creddefResponse.getCredentialDefinitionId();

        /* 3. Acme creates a Revocation Registry for the given Credential Definition.
         * 
         * The issuer anticipates revoking Job-Certificate credentials. It decides to create a revocation registry.
         * 
         * One of Hyperledger Indy’s revocation registry types uses cryptographic accumulators for publishing revoked credentials. 
         * The use of those accumulators requires the publication of “validity tails” outside of the Ledger.
         */

        RevRegCreateResponse createRegistryResponse = acme.revocationCreateRegistry(RevRegCreateRequest.builder()
                .credentialDefinitionId(ctx.acmeJobCertificateCredDefId)
                .build()).get();
        log.info("{}", createRegistryResponse);

        ctx.acmeJobCertificateRevocationRegistryId = createRegistryResponse.getRevocRegId();

        // 4. Acme creates a Revocation Registry for the given Credential Definition.

//        String revRegDefTag = "Tag2";
//        String revRegDefConfig = new JSONObject().put("issuance_type", "ISSUANCE_ON_DEMAND").put("max_cred_num", 5).toString();
//        IssuerCreateAndStoreRevocRegResult createRevRegResult = Anoncreds.issuerCreateAndStoreRevocReg(ctx.acmeWallet, ctx.acmeDid, null, revRegDefTag, ctx.jobCertificateCredDefId, revRegDefConfig, tailsWriter).get();
//        String revRegEntryJson = createRevRegResult.getRevRegEntryJson();
//        String revRegDefJson = createRevRegResult.getRevRegDefJson();
//        ctx.revocRegistryId = createRevRegResult.getRevRegId();

        // 5. Acme creates and submits the Revocation Registry Definition

//        String revRegDefRequest = Ledger.buildRevocRegDefRequest(ctx.acmeDid, revRegDefJson).get();
//        String revRegDefResponse = signAndSubmitRequest(ctx, ctx.acmeWallet, ctx.acmeDid, revRegDefRequest);
//        log.info(revRegDefResponse);

        // 6. Acme creates and submits the Revocation Registry Entry

//        String revRegEntryRequest = Ledger.buildRevocRegEntryRequest(ctx.acmeDid, ctx.revocRegistryId, "CL_ACCUM", revRegEntryJson).get();
//        String revRegEntryResponse = signAndSubmitRequest(ctx, ctx.acmeWallet, ctx.acmeDid, revRegEntryRequest);
//        log.info(revRegEntryResponse);
    }

    void getTranscriptFromFaber(Context ctx) throws Exception {

        /* 1. Faber creates a Credential Offer for Alice
         *
         * The value of this Transcript Credential is that it is provably issued by Faber College
         */

        // Create client for sub wallet
        AriesClient faber = useWallet(ctx.faberWallet);

        ConnectionStaticResult connectionResult = faber.connectionsCreateStatic(ConnectionStaticRequest.builder()
                .theirDid(ctx.aliceDid.getDid())
                .theirVerkey(ctx.aliceDid.getVerkey())
                .build()).get();
        ConnectionRecord record = connectionResult.getRecord();
        log.info("{} {}", record.getState(), connectionResult);
        ctx.faberAliceConnectionId = record.getConnectionId();

        V1CredentialExchange credentialExchange = faber.issueCredentialSend(V1CredentialProposalRequest.builder()
                .connectionId(ctx.faberAliceConnectionId)
                .credentialDefinitionId(ctx.faberTranscriptCredDefId)
                .credentialProposal(new CredentialPreview(CredentialAttributes.from(Map.of(
                        "first_name", "Alice", 
                        "last_name", "Garcia", 
                        "degree", "Bachelor of Science, Marketing", 
                        "status", "graduated", 
                        "ssn", "123-45-6789", 
                        "year", "2015", 
                        "average", "5"))))
                .build()).get();
        log.info("{}", credentialExchange);

        // Create client for sub wallet
        AriesClient alice = useWallet(ctx.aliceWallet);

        for (V1CredentialExchange credex : alice.issueCredentialRecords(null).get()) {
            log.info("{}", credex);
        }

        /*
         * 2. Alice gets Credential Definition from Ledger
         * 
         * Alice wants to see the attributes that the Transcript Credential contains.
         * These attributes are known because a Credential Schema for Transcript has
         * been written to the Ledger.
         */

//		String getSchemaRequest = Ledger.buildGetSchemaRequest(ctx.faberDidForAlice, ctx.transcriptSchemaId).get();
//		String getSchemaResponse = Ledger.submitRequest(ctx.pool, getSchemaRequest).get();
//		ParseResponseResult parseSchemaResult = Ledger.parseGetSchemaResponse(getSchemaResponse).get();
//		log.info("Transcript Schema" + parseSchemaResult.getObjectJson());

        /*
         * 3. Alice creates a Master Secret
         * 
         * A Master Secret is an item of Private Data used by a Prover to guarantee that
         * a credential uniquely applies to them.
         * 
         * The Master Secret is an input that combines data from multiple Credentials to
         * prove that the Credentials have a common subject (the Prover). A Master
         * Secret should be known only to the Prover.
         */

//		ctx.aliceMasterSecretId = Anoncreds.proverCreateMasterSecret(ctx.aliceWallet, null).get();

        /*
         * 4. Alice get the Credential Definition
         * 
         * Alice also needs to get the Credential Definition corresponding to the
         * Credential Definition Id in the Transcript Credential Offer.
         */

//		String credDefResponse = submitRequest(ctx, Ledger.buildGetCredDefRequest(ctx.aliceDid, transcriptCredDefId).get());
//		ParseResponseResult parsedCredDefResponse = Ledger.parseGetCredDefResponse(credDefResponse).get();
//		String transcriptCredDef = parsedCredDefResponse.getObjectJson();

        // 5. Alice creates a Credential Request of the issuance of the Transcript
        // Credential

//		ProverCreateCredentialRequestResult credentialRequestResult = Anoncreds.proverCreateCredentialReq(ctx.aliceWallet, ctx.aliceDidForFaber, transcriptCredOffer, transcriptCredDef, ctx.aliceMasterSecretId).get();
//		String credentialRequestMetadataJson = credentialRequestResult.getCredentialRequestMetadataJson();
//		String credentialRequestJson = credentialRequestResult.getCredentialRequestJson();

        /*
         * 6. Faber creates the Transcript Credential for Alice
         * 
         * Encoding is not standardized by Indy except that 32-bit integers are encoded
         * as themselves.
         */

//		String credValuesJson = new JSONObject()
//			.put("first_name", new JSONObject().put("raw", "Alice").put("encoded", "1139481716457488690172217916278103335"))
//			.put("last_name", new JSONObject().put("raw", "Garcia").put("encoded", "5321642780241790123587902456789123452"))
//			.put("degree", new JSONObject().put("raw", "Bachelor of Science, Marketing").put("encoded", "12434523576212321"))
//			.put("status", new JSONObject().put("raw", "graduated").put("encoded", "2213454313412354"))
//			.put("ssn", new JSONObject().put("raw", "123-45-6789").put("encoded", "3124141231422543541"))
//			.put("year", new JSONObject().put("raw", "2015").put("encoded", "2015"))
//			.put("average", new JSONObject().put("raw", "5").put("encoded", "5")).toString();
//		
//		IssuerCreateCredentialResult issuerCredentialResult = Anoncreds.issuerCreateCredential(ctx.faberWallet, transcriptCredOffer, credentialRequestJson, credValuesJson, null, 0).get();
//		String transcriptCredJson = issuerCredentialResult.getCredentialJson();
//		log.info("IssuedCredential: " + transcriptCredJson);

        // 7. Alice stores Transcript Credential from Faber in her Wallet

//		String transcriptCredentialId = Anoncreds.proverStoreCredential(ctx.aliceWallet, null, credentialRequestMetadataJson, transcriptCredJson, transcriptCredDef, null).get();
//		log.info("Transcript Credential Id: " + transcriptCredentialId);
    }

    private void closeAndDeleteWallets(Context ctx) throws IOException {
        removeWallet(ctx.governmentWallet);
        removeWallet(ctx.faberWallet);
        removeWallet(ctx.acmeWallet);
        removeWallet(ctx.thriftWallet);
        removeWallet(ctx.aliceWallet);
    }
}
