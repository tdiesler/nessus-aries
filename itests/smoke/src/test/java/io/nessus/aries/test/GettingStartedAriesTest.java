package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.TRUSTEE;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.hyperledger.acy_py.generated.model.ConnectionInvitation;
import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionReceiveInvitationFilter;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.connection.CreateInvitationParams;
import org.hyperledger.aries.api.connection.CreateInvitationRequest;
import org.hyperledger.aries.api.connection.CreateInvitationResponse;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionResponse;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange.CredentialProposalDict.CredentialProposal;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialIssueRequest;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialOfferRequest;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialStoreRequest;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.api.schema.SchemaSendRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.aries.api.schema.SchemaSendResponse.Schema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nessus.aries.common.CredentialProposalHelper;
import io.nessus.aries.common.V1CredentialExchangeHelper;
import io.nessus.aries.common.websocket.EventSubscriber;
import io.nessus.aries.common.websocket.WebSocketEventHandler;
import io.nessus.aries.common.websocket.WebSocketEventHandler.WebSocketEvent;
import io.nessus.aries.common.websocket.WebSockets;
import okhttp3.WebSocket;

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
        WebSocket governmentWebSocket;

        DID faberDid;
        WalletRecord faberWallet;
        String faberTranscriptCredDefId;
        WebSocket faberWebSocket;
        ConnectionRecord faberAliceConnection;

        DID acmeDid;
        WalletRecord acmeWallet;
        String acmeJobCertificateCredDefId;
        String acmeJobCertificateRevocationRegistryId;
        WebSocket acmeWebSocket;

        DID thriftDid;
        WalletRecord thriftWallet;
        WebSocket thriftWebSocket;

        WalletRecord aliceWallet;
        WebSocket aliceWebSocket;
        ConnectionRecord aliceFaberConnection;
        
        String transcriptSchemaId;
        String jobCertificateSchemaId;
    }
    
    @Test
    public void testWorkflow() throws Exception {
        Context ctx = new Context();
        try {
            doWorkflow(ctx);
        } finally {
            closeAndDeleteWallets(ctx);
        }
    }

    void doWorkflow(Context ctx) throws Exception {
        
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

        onboardFaberCollege(ctx);
        onboardAcmeCorp(ctx);
        //onboardThriftBank(ctx);
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
        //createJobCertificateSchema(ctx);

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
        //createJobCertificateCredentialDefinition(ctx);

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

        getTranscriptFromFaber(ctx);

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

//          applyForJobWithAcme(ctx);

        /*
         * Alice applies for a loan with Thrift Bank
         * 
         * Now that Alice has a job, she’d like to apply for a loan. That will require a
         * proof of employment. She can get this from the Job-Certificate credential
         * offered by Acme.
         */

//          applyForLoanWithThrift(ctx);

        /*
         * Thrift accepts the loan application and now requires KYC
         * 
         * Thrift Bank sends the second Proof Request where Alice needs to share her
         * personal information with the bank.
         */

//          kycProcessWithThrift(ctx);

        /*
         * Alice decides to quit her job with Acme
         */

//          quitJobWithAcme(ctx);
    }

    private void onboardGovernment(Context ctx) throws IOException {

        logSection("Onboard Government");
        
        WalletRecord wallet = new WalletBuilder("Government")
                .ledgerRole(TRUSTEE).selfRegisterNym().build();

        // Create client for sub wallet
        AriesClient client = createClient(wallet);
        DID publicDid = client.walletDidPublic().get();

        ctx.governmentWallet = wallet;
        ctx.governmentDid = publicDid;
        ctx.governmentWebSocket = WebSockets.createWebSocket(wallet, new WebSocketEventHandler.Builder().walletRegistry(walletRegistry).build());
    }

    private void onboardFaberCollege(Context ctx) throws IOException {

        logSection("Onboard Faber");
        
        WalletRecord wallet = new WalletBuilder("Faber")
                .registerNym(ctx.governmentWallet).ledgerRole(ENDORSER).build();

        // Create client for sub wallet
        AriesClient client = createClient(wallet);
        DID publicDid = client.walletDidPublic().get();

        ctx.faberWallet = wallet;
        ctx.faberDid = publicDid;
        ctx.faberWebSocket = WebSockets.createWebSocket(wallet, new WebSocketEventHandler.Builder().walletRegistry(walletRegistry).build());
    }

    private void onboardAcmeCorp(Context ctx) throws IOException {

        logSection("Onboard Acme");
        
        WalletRecord wallet = new WalletBuilder("Acme")
                .registerNym(ctx.governmentWallet).ledgerRole(ENDORSER).build();

        // Create client for sub wallet
        AriesClient client = createClient(wallet);
        DID publicDid = client.walletDidPublic().get();

        ctx.acmeWallet = wallet;
        ctx.acmeDid = publicDid;
        ctx.acmeWebSocket = WebSockets.createWebSocket(wallet, new WebSocketEventHandler.Builder().walletRegistry(walletRegistry).build());
    }

    private void onboardThriftBank(Context ctx) throws IOException {

        logSection("Onboard Thrift");
        
        WalletRecord wallet = new WalletBuilder("Thrift")
                .registerNym(ctx.governmentWallet).ledgerRole(ENDORSER).build();

        // Create client for sub wallet
        AriesClient client = createClient(wallet);
        DID publicDid = client.walletDidPublic().get();

        ctx.thriftWallet = wallet;
        ctx.thriftDid = publicDid;
        ctx.thriftWebSocket = WebSockets.createWebSocket(wallet, new WebSocketEventHandler.Builder().walletRegistry(walletRegistry).build());
    }

    private void onboardAlice(Context ctx) throws IOException {

        logSection("Onboard Alice");
        
        WalletRecord wallet = new WalletBuilder("Alice").build();
        ctx.aliceWallet = wallet;
        ctx.aliceWebSocket = WebSockets.createWebSocket(wallet, new WebSocketEventHandler.Builder().walletRegistry(walletRegistry).build());
    }

    private void connectAliceToFaber(Context ctx) throws Exception {
        
        logSection("Connect Alice to Faber");
        
        Map<String, ConnectionRecord> connections = new HashMap<>();
        CountDownLatch peerConnectionLatch = new CountDownLatch(2);
        
        Consumer<WebSocketEvent> eventConsumer = ev -> {
            ConnectionRecord con = ev.getPayload(ConnectionRecord.class);
            log.info("{}: [@{}] {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), con.getTheirRole(), con.getState(), con);
            connections.put(ev.getTheirWalletId(), con);
            if (ConnectionState.ACTIVE == con.getState()) {
                peerConnectionLatch.countDown();
            }
        };
        
        String faberWalletId = ctx.faberWallet.getWalletId();
        String aliceWalletId = ctx.aliceWallet.getWalletId();
        
        WebSocketEventHandler faberHandler = WebSockets.getEventHandler(ctx.faberWebSocket);
        EventSubscriber<WebSocketEvent> faberSubscriber = faberHandler.subscribe(aliceWalletId, ConnectionRecord.class, eventConsumer);
        
        WebSocketEventHandler aliceHandler = WebSockets.getEventHandler(ctx.aliceWebSocket);
        EventSubscriber<WebSocketEvent> aliceSubscriber = aliceHandler.subscribe(faberWalletId, ConnectionRecord.class, eventConsumer);
        
        AriesClient faber = createClient(ctx.faberWallet);
        AriesClient alice = createClient(ctx.aliceWallet);
        
        // Inviter creates an invitation (/connections/create-invitation)
        CreateInvitationResponse response = faber.connectionsCreateInvitation(
                CreateInvitationRequest.builder().build(), 
                CreateInvitationParams.builder()
                    .autoAccept(true)
                    .build()).get();
        ConnectionInvitation invitation = response.getInvitation();
        
        // Invitee receives the invitation from the Inviter (/connections/receive-invitation)
        alice.connectionsReceiveInvitation(ReceiveInvitationRequest.builder()
                .recipientKeys(invitation.getRecipientKeys())
                .serviceEndpoint(invitation.getServiceEndpoint())
                .build(), ConnectionReceiveInvitationFilter.builder()
                    .autoAccept(true)
                    .build()).get();

        Assertions.assertTrue(peerConnectionLatch.await(10, TimeUnit.SECONDS), "NO ACTIVE connections");
        
        faberSubscriber.cancelSubscription();
        aliceSubscriber.cancelSubscription();
        
        ctx.faberAliceConnection = connections.get(faberWalletId);
        ctx.aliceFaberConnection = connections.get(aliceWalletId);
    }

    private void createTranscriptSchema(Context ctx) throws IOException {

        logSection("Create Transcript Schema");
        
        // Government creates the Transcript Credential Schema and sends it to the Ledger
        // It can do so with it's Endorser role

        // Create client for sub wallet
        AriesClient client = createClient(ctx.governmentWallet);

        SchemaSendResponse schemaResponse = client.schemas(SchemaSendRequest.builder()
                .schemaVersion("1.2")
                .schemaName("Transcript")
                .attributes(Arrays.asList("first_name", "last_name", "degree", "status", "year", "average", "ssn"))
                .build()).get();
        log.info("{}", schemaResponse);

        ctx.transcriptSchemaId = schemaResponse.getSchemaId();
    }

    void createJobCertificateSchema(Context ctx) throws Exception {

        logSection("Create Job Certificate Schema");
        
        // Government creates the Job-Certificate Credential Schema and sends it to the Ledger
        // It can do so with it's Endorser role

        // Create client for sub wallet
        AriesClient client = createClient(ctx.governmentWallet);

        SchemaSendResponse schemaResponse = client.schemas(SchemaSendRequest.builder()
                .schemaVersion("0.2")
                .schemaName("Job-Certificate")
                .attributes(Arrays.asList("first_name", "last_name", "salary", "employee_status", "experience"))
                .build()).get();
        log.info("{}", schemaResponse);

        ctx.jobCertificateSchemaId = schemaResponse.getSchemaId();
    }

    void createTranscriptCredentialDefinition(Context ctx) throws Exception {

        logSection("Create Transcript CredDef");
        
        // 1. Faber get the Transcript Credential Schema

        // Create client for sub wallet
        AriesClient faber = createClient(ctx.faberWallet);

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

        logSection("Create Job Certificate CredDef");
        
        // 1. Acme get the Transcript Credential Schema

        // Create client for sub wallet
        AriesClient acme = createClient(ctx.acmeWallet);

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

//        RevRegCreateResponse createRegistryResponse = acme.revocationCreateRegistry(RevRegCreateRequest.builder()
//                .credentialDefinitionId(ctx.acmeJobCertificateCredDefId)
//                .build()).get();
//        log.info("{}", createRegistryResponse);
//
//        ctx.acmeJobCertificateRevocationRegistryId = createRegistryResponse.getRevocRegId();

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

        connectAliceToFaber(ctx);

        logSection("Alice gets Transcript from Faber");
        
        AriesClient faber = createClient(ctx.faberWallet);
        V1CredentialExchangeHelper faberCredentialHelper = new V1CredentialExchangeHelper(ctx.faberWallet);
        
        AriesClient alice = createClient(ctx.aliceWallet);
        V1CredentialExchangeHelper aliceCredentialHelper = new V1CredentialExchangeHelper(ctx.aliceWallet);
        
        /* 1. Faber sends the Transcript Credential Offer
         * 
         * The value of this Transcript Credential is that it is provably issued by Faber College
         */
        
        V1CredentialExchange credex = faber.issueCredentialSendOffer(V1CredentialOfferRequest.builder()
                .connectionId(ctx.faberAliceConnection.getConnectionId())
                .credentialDefinitionId(ctx.faberTranscriptCredDefId)
                .credentialPreview(new CredentialPreview(CredentialAttributes.from(Map.of(
                        "first_name", "Alice", 
                        "last_name", "Garcia", 
                        "degree", "Bachelor of Science, Marketing", 
                        "status", "graduated", 
                        "ssn", "123-45-6789", 
                        "year", "2015", 
                        "average", "5"))))
                .build()).get();
        log.info("Faber: {} {} {}", credex.getRole(), credex.getState(), credex);
        
        /* 2. Alice inspects the the Transcript Credential Offer
         * 
         */
        
        credex = aliceCredentialHelper.awaitV1CredentialExchange(
                ctx.aliceFaberConnection.getConnectionId(), ctx.faberTranscriptCredDefId, 
                CredentialExchangeState.OFFER_RECEIVED, 10, TimeUnit.SECONDS).get();
        
        CredentialProposal credentialProposal = credex.getCredentialProposalDict().getCredentialProposal();
        log.info("{}", credentialProposal);
        
        CredentialProposalHelper credentialHelper = new CredentialProposalHelper(credentialProposal);
        Assertions.assertEquals("Alice", credentialHelper.getAttributeValue("first_name"));
        Assertions.assertEquals("Garcia", credentialHelper.getAttributeValue("last_name"));
        Assertions.assertEquals("graduated", credentialHelper.getAttributeValue("status"));
        Assertions.assertEquals("5", credentialHelper.getAttributeValue("average"));
        
        /* 3. Alice sends the Transcript Credential Request
         * 
         */
        
        credex = alice.issueCredentialRecordsSendRequest(credex.getCredentialExchangeId()).get();
        log.info("Alice: {} {} {}", credex.getRole(), credex.getState(), credex);
        
        /* 4. Faber receives the Transcript Credential Request
         * 
         */

        credex = faberCredentialHelper.awaitV1CredentialExchange(
                ctx.faberAliceConnection.getConnectionId(), ctx.faberTranscriptCredDefId, 
                CredentialExchangeState.REQUEST_RECEIVED, 10, TimeUnit.SECONDS).get();
        
        /* 5. Faber issues the Transcript Credential
         * 
         */

        credex = faber.issueCredentialRecordsIssue(credex.getCredentialExchangeId(), V1CredentialIssueRequest.builder().build()).get();
        log.info("Faber: {} {} {}", credex.getRole(), credex.getState(), credex);
        
        /* 6. Alice receives the Transcript Credential
         * 
         */

        credex = aliceCredentialHelper.awaitV1CredentialExchange(
                ctx.aliceFaberConnection.getConnectionId(), ctx.faberTranscriptCredDefId, 
                CredentialExchangeState.CREDENTIAL_RECEIVED, 10, TimeUnit.SECONDS).get();
        
        /* 7. Alice stores the Transcript Credential
         * 
         */

        credex = alice.issueCredentialRecordsStore(credex.getCredentialExchangeId(), V1CredentialStoreRequest.builder()
                .credentialId(credex.getCredentialId())
                .build()).get();
        log.info("Alice: {} {} {}", credex.getRole(), credex.getState(), credex);
    }

    private void closeAndDeleteWallets(Context ctx) throws IOException {
        
        logSection("Remove Wallets");
        
        removeWallet(ctx.governmentWallet);
        removeWallet(ctx.faberWallet);
        removeWallet(ctx.acmeWallet);
        removeWallet(ctx.thriftWallet);
        removeWallet(ctx.aliceWallet);
    }
}
