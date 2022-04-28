package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.TRUSTEE;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.acy_py.generated.model.IndyProofReqPredSpec.PTypeEnum;
import org.hyperledger.acy_py.generated.model.IssuerRevRegRecord;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionResponse;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.IssueCredentialRecordsFilter;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange.CredentialProposalDict.CredentialProposal;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialIssueRequest;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialOfferRequest;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialStoreRequest;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof.PresentProofRequest.ProofRequest;
import org.hyperledger.aries.api.present_proof.PresentProofRequest.ProofRequest.ProofNonRevoked;
import org.hyperledger.aries.api.present_proof.PresentProofRequest.ProofRequest.ProofRequestedAttributes;
import org.hyperledger.aries.api.present_proof.PresentProofRequest.ProofRequest.ProofRequestedPredicates;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.aries.api.present_proof.PresentationRequest;
import org.hyperledger.aries.api.present_proof.PresentationRequest.IndyRequestedCredsRequestedAttr;
import org.hyperledger.aries.api.present_proof.PresentationRequest.IndyRequestedCredsRequestedPred;
import org.hyperledger.aries.api.present_proof.PresentationRequestCredentials.CredentialInfo;
import org.hyperledger.aries.api.revocation.RevRegCreateRequest;
import org.hyperledger.aries.api.revocation.RevocationEvent;
import org.hyperledger.aries.api.revocation.RevokeRequest;
import org.hyperledger.aries.api.schema.SchemaSendRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.aries.api.schema.SchemaSendResponse.Schema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.nessus.aries.coms.EventSubscriber;
import io.nessus.aries.coms.WebSocketEventHandler;
import io.nessus.aries.coms.WebSocketEventHandler.WebSocketEvent;
import io.nessus.aries.coms.WebSockets;
import io.nessus.aries.util.AttachmentKey;
import io.nessus.aries.util.AttachmentSupport;
import io.nessus.aries.wallet.ConnectionHelper;
import io.nessus.aries.wallet.CredentialProposalHelper;
import okhttp3.WebSocket;

/**
 * The Ledger is externally provided by a running instance of the VON-Network
 * The Agent is Aries Cloudagent Python
 * 
 * docker-compose up --detach && docker-compose logs -f acapy
 */
public class GettingStartedTest extends AbstractAriesTest {

    static final String Government = "Government";
    static final String Faber = "Faber";
    static final String Acme = "Acme";
    static final String Thrift = "Thrift";
    static final String Alice = "Alice";
    
    static final String TranscriptSchemaId = "TranscriptSchemaId";
    static final String TranscriptCredDefId = "TranscriptSchemaId";
    static final String JobCertificateSchemaId = "JobCertificateSchemaId";
    static final String JobCertificateCredDefId = "JobCertificateCredDefId";
    
    class Context extends AttachmentSupport {

        DID getDID(String name) {
            return getAttachment(name, DID.class);
        }
        
        ConnectionRecord getConnection(String inviter, String invitee) {
            return getAttachment(inviter + invitee + "Connection", ConnectionRecord.class);
        }

        String getIdentifier(String name) {
            return getAttachment(name, String.class);
        }
        
        WalletRecord getWallet(String name) {
            return getAttachment(name, WalletRecord.class);
        }
        
        WebSocket getWebSocket(String name) {
            return getAttachment(name, WebSocket.class);
        }
        
        <T> T getAttachment(String name, Class<T> type) {
            return getAttachment(new AttachmentKey<>(name, type));
        }
        
        <T> T putAttachment(String name,  Class<T> type, T obj) {
            return putAttachment(new AttachmentKey<T>(name, type), obj);
        }
        
        @SuppressWarnings("unchecked")
        <T> T putAttachment(String name,  T obj) {
            return putAttachment(new AttachmentKey<T>(name, (Class<T>) obj.getClass()), obj);
        }
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

        createTranscriptSchema(ctx);
        createJobCertificateSchema(ctx);

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
        createJobCertificateCredentialDefinition(ctx);

        /*
         * Create a peer connection between Alice/Faber
         * 
         * Alice does not connect to Faber's public DID, Alice does not even have a public DID
         * Instead both parties create new DIDs that they use for their peer connection 
         */
        
        connectPeers(ctx, Faber, Alice);

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
         * Create a peer connection between Alice/Acme
         * 
         *  Alice does not connect to Faber's public DID, Alice does not even have a public DID
         *  Instead both parties create new DIDs that they use for their peer connection 
         */
         
        connectPeers(ctx, Acme, Alice);

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

        applyForJobWithAcme(ctx);

        /*
         * Alice gets the job and hence receives a JobCertificate from Acme
         * 
         * This is similar to the Transcript VC that she got from Faber, except that the 
         * JobCertificate credential can be revoked by Acme  
         */
        
        getJobWithAcme(ctx); 

        /*
         * Create a peer connection between Alice/Faber
         * 
         * Alice does not connect to Faber's public DID, Alice does not even have a public DID
         * Instead both parties create new DIDs that they use for their peer connection 
         */
         
        connectPeers(ctx, Thrift, Alice);

        /*
         * Alice applies for a loan with Thrift Bank
         * 
         * Now that Alice has a job, she’d like to apply for a loan. That will require a
         * proof of employment. She can get this from the Job-Certificate credential
         * offered by Acme.
         */

        applyForLoanWithThrift(ctx);

        /*
         * Thrift accepts the loan application and now requires KYC
         * 
         * Thrift Bank sends the second Proof Request where Alice needs to share her
         * personal information with the bank.
         */

        kycProcessWithThrift(ctx);

        /*
         * Alice decides to quit her job with Acme
         */

        acmeRevokesTheJobCertificate(ctx);

        /*
         * Alice applies for another loan with Thrift Bank - this time without having a Job
         * 
         */

        applyForLoanWithThrift(ctx);
    }

    void onboardGovernment(Context ctx) throws IOException {

        logSection("Onboard " + Government);
        
        WalletRecord wallet = new WalletBuilder(Government)
                .ledgerRole(TRUSTEE).selfRegisterNym().build();

        // Create client for sub wallet
        AriesClient client = createClient(wallet);
        DID publicDid = client.walletDidPublic().get();

        WebSocket webSocket = WebSockets.createWebSocket(wallet, new WebSocketEventHandler.Builder()
                .subscribe(Arrays.asList(), ev -> log.debug("{}: [@{}] {}", ev.getThisWalletName(), ev.getTheirWalletName(), ev.getPayload()))
                .walletRegistry(walletRegistry)
                .build());
        
        ctx.putAttachment(Government, wallet);
        ctx.putAttachment(Government, publicDid);
        ctx.putAttachment(Government, WebSocket.class, webSocket);
    }

    void onboardFaberCollege(Context ctx) throws IOException {

        logSection("Onboard " + Faber);
        
        WalletRecord wallet = new WalletBuilder(Faber)
                .registerNym(ctx.getWallet(Government)).ledgerRole(ENDORSER).build();

        // Create client for sub wallet
        AriesClient client = createClient(wallet);
        DID publicDid = client.walletDidPublic().get();

        WebSocket webSocket = WebSockets.createWebSocket(wallet, new WebSocketEventHandler.Builder()
                .subscribe(Arrays.asList(), ev -> log.debug("{}: [@{}] {}", ev.getThisWalletName(), ev.getTheirWalletName(), ev.getPayload()))
                .walletRegistry(walletRegistry)
                .build());
        
        ctx.putAttachment(Faber, wallet);
        ctx.putAttachment(Faber, publicDid);
        ctx.putAttachment(Faber, WebSocket.class, webSocket);
    }

    void onboardAcmeCorp(Context ctx) throws IOException {

        logSection("Onboard " + Acme);
        
        WalletRecord wallet = new WalletBuilder(Acme)
                .registerNym(ctx.getWallet(Government)).ledgerRole(ENDORSER).build();

        // Create client for sub wallet
        AriesClient client = createClient(wallet);
        DID publicDid = client.walletDidPublic().get();

        WebSocket webSocket = WebSockets.createWebSocket(wallet, new WebSocketEventHandler.Builder()
                .subscribe(Arrays.asList(), ev -> log.debug("{}: [@{}] {}", ev.getThisWalletName(), ev.getTheirWalletName(), ev.getPayload()))
                .walletRegistry(walletRegistry)
                .build());
        
        ctx.putAttachment(Acme, wallet);
        ctx.putAttachment(Acme, publicDid);
        ctx.putAttachment(Acme, WebSocket.class, webSocket);
    }

    void onboardThriftBank(Context ctx) throws IOException {

        logSection("Onboard " + Thrift);
        
        WalletRecord wallet = new WalletBuilder(Thrift)
                .registerNym(ctx.getWallet(Government)).ledgerRole(ENDORSER).build();

        // Create client for sub wallet
        AriesClient client = createClient(wallet);
        DID publicDid = client.walletDidPublic().get();

        WebSocket webSocket = WebSockets.createWebSocket(wallet, new WebSocketEventHandler.Builder()
                .subscribe(Arrays.asList(), ev -> log.debug("{}: [@{}] {}", ev.getThisWalletName(), ev.getTheirWalletName(), ev.getPayload()))
                .walletRegistry(walletRegistry)
                .build());
        
        ctx.putAttachment(Thrift, wallet);
        ctx.putAttachment(Thrift, publicDid);
        ctx.putAttachment(Thrift, WebSocket.class, webSocket);
    }

    void onboardAlice(Context ctx) throws IOException {

        logSection("Onboard " + Alice);
        
        WalletRecord wallet = new WalletBuilder(Alice).build();
        
        WebSocket webSocket = WebSockets.createWebSocket(wallet, new WebSocketEventHandler.Builder()
                .subscribe(Arrays.asList(), ev -> log.debug("{}: [@{}] {}", ev.getThisWalletName(), ev.getTheirWalletName(), ev.getPayload()))
                .walletRegistry(walletRegistry)
                .build());
        
        ctx.putAttachment(Alice, wallet);
        ctx.putAttachment(Alice, WebSocket.class, webSocket);
    }

    void connectPeers(Context ctx, String inviter, String invitee) throws Exception {
        
        logSection(String.format("Connect %s to %s", inviter, invitee));
        
        WalletRecord inviterWallet = ctx.getWallet(inviter);
        String inviterId = inviterWallet.getWalletId();        

        WalletRecord inviteeWallet = ctx.getWallet(invitee);
        String inviteeId = inviteeWallet.getWalletId();        
        
        Map<String, ConnectionRecord> connections = ConnectionHelper.connectPeers(inviterWallet, inviteeWallet);
        
        ctx.putAttachment(inviter + invitee + "Connection", connections.get(inviterId));
        ctx.putAttachment(invitee + inviter + "Connection", connections.get(inviteeId));
    }

    void createTranscriptSchema(Context ctx) throws IOException {

        logSection("Create Transcript Schema");
        
        // Government creates the Transcript Credential Schema and sends it to the Ledger
        // It can do so with it's Endorser role

        // Create client for sub wallet
        AriesClient client = createClient(ctx.getWallet(Government));

        SchemaSendResponse schemaResponse = client.schemas(SchemaSendRequest.builder()
                .schemaVersion("1.2")
                .schemaName("Transcript")
                .attributes(Arrays.asList("first_name", "last_name", "degree", "status", "year", "average", "ssn"))
                .build()).get();
        log.info("{}", schemaResponse);

        ctx.putAttachment(TranscriptSchemaId, schemaResponse.getSchemaId());
    }

    void createJobCertificateSchema(Context ctx) throws Exception {

        logSection("Create Job Certificate Schema");
        
        // Government creates the Job-Certificate Credential Schema and sends it to the Ledger
        // It can do so with it's Trustee role

        // Create client for sub wallet
        AriesClient client = createClient(ctx.getWallet(Government));

        SchemaSendResponse schemaResponse = client.schemas(SchemaSendRequest.builder()
                .schemaVersion("0.2")
                .schemaName("Job-Certificate")
                .attributes(Arrays.asList("first_name", "last_name", "salary", "employee_status", "experience"))
                .build()).get();
        log.info("{}", schemaResponse);

        ctx.putAttachment(JobCertificateSchemaId, schemaResponse.getSchemaId());
    }

    void createTranscriptCredentialDefinition(Context ctx) throws Exception {

        logSection("Create Transcript CredDef");
        
        // 1. Faber get the Transcript Credential Schema

        AriesClient faber = createClient(ctx.getWallet(Faber));
        Schema schema = faber.schemasGetById(ctx.getIdentifier(TranscriptSchemaId)).get();
        log.info("{}", schema);

        // 2. Faber creates the Credential Definition related to the received Credential Schema and send it to the ledger

        CredentialDefinitionResponse creddefResponse = faber.credentialDefinitionsCreate(CredentialDefinitionRequest.builder()
                .schemaId(schema.getId())
                .supportRevocation(false)
                .build()).get();
        log.info("{}", creddefResponse);

        ctx.putAttachment(TranscriptCredDefId, creddefResponse.getCredentialDefinitionId());
    }

    void createJobCertificateCredentialDefinition(Context ctx) throws Exception {

        logSection("Create Job Certificate CredDef");
        
        // 1. Acme get the Transcript Credential Schema

        AriesClient acme = createClient(ctx.getWallet(Acme));
        Schema schema = acme.schemasGetById(ctx.getIdentifier(JobCertificateSchemaId)).get();
        log.info("{}", schema);

        // 2. Acme creates the Credential Definition related to the received Credential Schema and send it to the ledger

        CredentialDefinitionResponse creddefResponse = acme.credentialDefinitionsCreate(CredentialDefinitionRequest.builder()
                .schemaId(schema.getId())
                .supportRevocation(true)
                .build()).get();
        log.info("{}", creddefResponse);

        String credentialDefinitionId = creddefResponse.getCredentialDefinitionId();
        ctx.putAttachment(JobCertificateCredDefId, credentialDefinitionId);

        /* 3. Acme creates a Revocation Registry for the given Credential Definition.
         * 
         * The issuer anticipates revoking Job-Certificate credentials. It decides to create a revocation registry.
         * 
         * One of Hyperledger Indy’s revocation registry types uses cryptographic accumulators for publishing revoked credentials. 
         * The use of those accumulators requires the publication of “validity tails” outside of the Ledger.
         */

        IssuerRevRegRecord revocRegistryRecord = acme.revocationCreateRegistry(RevRegCreateRequest.builder()
                .credentialDefinitionId(credentialDefinitionId)
                .build()).get();
        log.info("{}", revocRegistryRecord);
    }

    void getTranscriptFromFaber(Context ctx) throws Exception {

        logSection("Alice gets Transcript from Faber");
        
        WalletRecord faberWallet = ctx.getWallet(Faber);
        String faberAliceConnectionId = ctx.getConnection(Faber, Alice).getConnectionId();
        AriesClient faber = createClient(faberWallet);
        
        WalletRecord aliceWallet = ctx.getWallet(Alice);
        AriesClient alice = createClient(aliceWallet);

        V1CredentialExchange[] issuerCredEx = new V1CredentialExchange[1];
        V1CredentialExchange[] holderCredEx = new V1CredentialExchange[1];
        CountDownLatch holderOfferReceived = new CountDownLatch(1);
        CountDownLatch issuerRequestReceived = new CountDownLatch(1);
        CountDownLatch holderCredentialReceived = new CountDownLatch(1);
        CountDownLatch holderCredentialAcked = new CountDownLatch(1);
        
        WebSocketEventHandler faberHandler = WebSockets.getEventHandler(ctx.getWebSocket(Faber));
        EventSubscriber<WebSocketEvent> faberSubscriber = faberHandler.subscribe(V1CredentialExchange.class, ev -> { 
            V1CredentialExchange cex = ev.getPayload(V1CredentialExchange.class);
            log.info("{}: [@{}] {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), cex.getRole(), cex.getState(), cex); 
            if (CredentialExchangeRole.ISSUER == cex.getRole() && CredentialExchangeState.REQUEST_RECEIVED == cex.getState()) {
                issuerCredEx[0] = cex;
                issuerRequestReceived.countDown();
            }
        });
        
        WebSocketEventHandler aliceHandler = WebSockets.getEventHandler(ctx.getWebSocket(Alice));
        EventSubscriber<WebSocketEvent> aliceSubscriber = aliceHandler.subscribe(V1CredentialExchange.class, ev -> { 
            V1CredentialExchange cex = ev.getPayload(V1CredentialExchange.class);
            log.info("{}: [@{}] {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), cex.getRole(), cex.getState(), cex);
            if (CredentialExchangeRole.HOLDER == cex.getRole() && CredentialExchangeState.OFFER_RECEIVED == cex.getState()) {
                holderCredEx[0] = cex;
                holderOfferReceived.countDown();
            }
            else if (CredentialExchangeRole.HOLDER == cex.getRole() && CredentialExchangeState.CREDENTIAL_RECEIVED == cex.getState()) {
                holderCredEx[0] = cex;
                holderCredentialReceived.countDown();
            }
            else if (CredentialExchangeRole.HOLDER == cex.getRole() && CredentialExchangeState.CREDENTIAL_ACKED == cex.getState()) {
                holderCredEx[0] = cex;
                holderCredentialAcked.countDown();
            }
        });

        /* 1. Faber sends the Transcript Credential Offer
         * 
         * The value of this Transcript Credential is that it is provably issued by Faber College
         */
        
        String transcriptCredDefId = ctx.getIdentifier(TranscriptCredDefId);
        faber.issueCredentialSendOffer(V1CredentialOfferRequest.builder()
                .connectionId(faberAliceConnectionId)
                .credentialDefinitionId(transcriptCredDefId)
                .credentialPreview(new CredentialPreview(CredentialAttributes.from(Map.of(
                        "first_name", "Alice", 
                        "last_name", "Garcia", 
                        "degree", "Bachelor of Science, Marketing", 
                        "status", "graduated", 
                        "ssn", "123-45-6789", 
                        "year", "2015", 
                        "average", "5"))))
                .build()).get();
        
        /* 2. Alice inspects the the Transcript Credential Offer
         * 
         */
        
        Assertions.assertTrue(holderOfferReceived.await(10, TimeUnit.SECONDS), "No HOLDER OFFER_RECEIVED");
        
        CredentialProposal credentialProposal = holderCredEx[0].getCredentialProposalDict().getCredentialProposal();
        CredentialProposalHelper credentialHelper = new CredentialProposalHelper(credentialProposal);
        Assertions.assertEquals("Alice", credentialHelper.getAttributeValue("first_name"));
        Assertions.assertEquals("Garcia", credentialHelper.getAttributeValue("last_name"));
        Assertions.assertEquals("graduated", credentialHelper.getAttributeValue("status"));
        Assertions.assertEquals("5", credentialHelper.getAttributeValue("average"));
        
        /* 3. Alice sends the Transcript Credential Request
         * 
         */
        
        alice.issueCredentialRecordsSendRequest(holderCredEx[0].getCredentialExchangeId()).get();
        
        /* 4. Faber receives the Transcript Credential Request
         * 
         */

        Assertions.assertTrue(issuerRequestReceived.await(10, TimeUnit.SECONDS), "No ISSUER REQUEST_RECEIVED");
        
        /* 5. Faber issues the Transcript Credential
         * 
         */

        faber.issueCredentialRecordsIssue(issuerCredEx[0].getCredentialExchangeId(), V1CredentialIssueRequest.builder().build()).get();
        
        /* 6. Alice receives the Transcript Credential
         * 
         */

        Assertions.assertTrue(holderCredentialReceived.await(10, TimeUnit.SECONDS), "No HOLDER CREDENTIAL_RECEIVED");
        
        /* 7. Alice stores the Transcript Credential
         * 
         */

        alice.issueCredentialRecordsStore(holderCredEx[0].getCredentialExchangeId(), V1CredentialStoreRequest.builder()
                .credentialId(holderCredEx[0].getCredentialId())
                .build()).get();

        Assertions.assertTrue(holderCredentialAcked.await(10, TimeUnit.SECONDS), "No HOLDER CREDENTIAL_ACKED");
        
        faberSubscriber.cancelSubscription();
        aliceSubscriber.cancelSubscription();
    }

    void applyForJobWithAcme(Context ctx) throws Exception {

        logSection("Alice applies for a Job with Acme");
        
        WalletRecord acmeWallet = ctx.getWallet(Acme);
        AriesClient acme = createClient(acmeWallet);
        
        WalletRecord aliceWallet = ctx.getWallet(Alice);
        AriesClient alice = createClient(aliceWallet);
        
        PresentationExchangeRecord[] proverExchangeRecord = new PresentationExchangeRecord[1];
        PresentationExchangeRecord[] verifierExchangeRecord = new PresentationExchangeRecord[1];
        CountDownLatch proverRequestReceived = new CountDownLatch(1);
        CountDownLatch verifierPresentationReceived = new CountDownLatch(1);
        CountDownLatch proverPresentationAcked = new CountDownLatch(1);
        
        WebSocketEventHandler acmeHandler = WebSockets.getEventHandler(ctx.getWebSocket(Acme));
        EventSubscriber<WebSocketEvent> acmeSubscriber = acmeHandler.subscribe(PresentationExchangeRecord.class, ev -> { 
            PresentationExchangeRecord pex = ev.getPayload(PresentationExchangeRecord.class);
            log.info("{}: [@{}] {} {} {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), pex.getRole(), pex.getState(), pex); 
            if (PresentationExchangeRole.VERIFIER == pex.getRole() && PresentationExchangeState.PRESENTATION_RECEIVED == pex.getState()) {
                verifierExchangeRecord[0] = pex;
                verifierPresentationReceived.countDown();
            }
        });
        
        WebSocketEventHandler aliceHandler = WebSockets.getEventHandler(ctx.getWebSocket(Alice));
        EventSubscriber<WebSocketEvent> aliceSubscriber = aliceHandler.subscribe(PresentationExchangeRecord.class, ev -> { 
            PresentationExchangeRecord pex = ev.getPayload(PresentationExchangeRecord.class);
            log.info("{}: [@{}] {} {} {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), pex.getRole(), pex.getState(), pex); 
            if (PresentationExchangeRole.PROVER == pex.getRole() && PresentationExchangeState.REQUEST_RECEIVED == pex.getState()) {
                proverExchangeRecord[0] = pex;
                proverRequestReceived.countDown();
            }
            if (PresentationExchangeRole.PROVER == pex.getRole() && PresentationExchangeState.PRESENTATION_ACKED == pex.getState()) {
                proverExchangeRecord[0] = pex;
                proverPresentationAcked.countDown();
            }
        });
       
        /* 1. Acme creates a Job Application Proof Request
         * 
         * Notice that some attributes are verifiable and others are not.
         * 
         * The proof request says that degree, and graduation status, ssn and year must be formally asserted by an issuer and schema_key. 
         * Notice also that the first_name, last_name and phone_number are not required to be verifiable. 
         * 
         * By not tagging these credentials with a verifiable status, Acme’s credential request is saying it will accept 
         * Alice’s own credential about her names and phone number.
         */
        
        String transcriptCredDefId = ctx.getIdentifier(TranscriptCredDefId);
        String acmeAliceConnectionId = ctx.getConnection(Acme, Alice).getConnectionId();
        
        Function<String, JsonObject> credDefRestriction = cdid -> gson.fromJson("{\"cred_def_id\"=\"" + cdid + "\"}", JsonObject.class);
        Function<String, ProofRequestedAttributes> proofReqAttr = name -> ProofRequestedAttributes.builder().name(name).build();
        BiFunction<String, String, ProofRequestedAttributes> restrictedProofReqAttr = (name, cdid) -> ProofRequestedAttributes.builder()
                .name(name)
                .restriction(credDefRestriction.apply(cdid))
                .build();
        BiFunction<String, String, ProofRequestedPredicates> restrictedProofReqPred = (pred, cdid) -> ProofRequestedPredicates.builder()
                .name(pred.split(" ")[0])
                .pType(PTypeEnum.fromValue(pred.split(" ")[1]))
                .pValue(Integer.valueOf(pred.split(" ")[2]))
                .restriction(credDefRestriction.apply(cdid))
                .build();
        
        acme.presentProofSendRequest(PresentProofRequest.builder()
                .connectionId(acmeAliceConnectionId)
                .proofRequest(ProofRequest.builder()
                        .name("Job-Application")
                        .nonce("1")
                        .requestedAttribute("attr1_referent", proofReqAttr.apply("first_name"))
                        .requestedAttribute("attr2_referent", proofReqAttr.apply("last_name"))
                        .requestedAttribute("attr3_referent", restrictedProofReqAttr.apply("degree", transcriptCredDefId))
                        .requestedAttribute("attr4_referent", restrictedProofReqAttr.apply("status", transcriptCredDefId))
                        .requestedAttribute("attr5_referent", restrictedProofReqAttr.apply("ssn", transcriptCredDefId))
                        .requestedAttribute("attr6_referent", restrictedProofReqAttr.apply("year", transcriptCredDefId))
                        .requestedPredicate("pred1_referent", restrictedProofReqPred.apply("average >= 4", transcriptCredDefId))
                        .build())
                .build()).get();
        
        Assertions.assertTrue(proverRequestReceived.await(10, TimeUnit.SECONDS), "No PROVER REQUEST_RECEIVED");

        // 2. Alice searches her Wallet for Credentials that she can use for the creating of Proof for the Job-Application Proof Request
        
        Map<String, String> referentMapping = new HashMap<>();
        String presentationExchangeId = proverExchangeRecord[0].getPresentationExchangeId();
        alice.presentProofRecordsCredentials(presentationExchangeId).get().stream()
            .forEach(cred -> {
                List<String> presentationReferents = cred.getPresentationReferents();
                CredentialInfo credInfo = cred.getCredentialInfo();
                String credDefId = credInfo.getCredentialDefinitionId();
                Map<String, String> attributes = credInfo.getAttrs();
                String referent = credInfo.getReferent();
                log.debug("{}", cred); 
                log.debug("+- CredDefId: {}", credDefId); 
                log.debug("+- PresentationReferents: {}", presentationReferents); 
                log.debug("+- Attributes: {}", attributes); 
                log.debug("+- Referent: {}", referent); 
                
                // Map attribute referents to their respective credential referent
                presentationReferents.stream().forEach(pr -> referentMapping.put(pr, referent));
            });

        /* 3. Alice provides Job Application Proof
         * 
         * Alice divides these attributes into the three groups:
         * 
         * - attributes values of which will be revealed
         * - attributes values of which will be unrevealed
         * - attributes for which creating of verifiable proof is not required
         */
        
        BiFunction<String, Boolean, Map<String, IndyRequestedCredsRequestedAttr>> indyRequestedAttr = (ref, reveal) -> Map.of(ref, IndyRequestedCredsRequestedAttr.builder()
                .credId(referentMapping.get(ref))
                .revealed(reveal)
                .build());
        Function<String, Map<String, IndyRequestedCredsRequestedPred>> indyRequestedPred = ref  -> Map.of(ref, IndyRequestedCredsRequestedPred.builder()
                .credId(referentMapping.get(ref))
                .build());
        
        alice.presentProofRecordsSendPresentation(presentationExchangeId, PresentationRequest.builder()
                .selfAttestedAttributes(Map.of(
                        "attr1_referent", "Alice", 
                        "attr2_referent", "Garcia"))
                .requestedAttributes(indyRequestedAttr.apply("attr3_referent", true))
                .requestedAttributes(indyRequestedAttr.apply("attr4_referent", true))
                .requestedAttributes(indyRequestedAttr.apply("attr5_referent", false))
                .requestedAttributes(indyRequestedAttr.apply("attr6_referent", false))
                .requestedPredicates(indyRequestedPred.apply("pred1_referent"))
                .build());

        Assertions.assertTrue(verifierPresentationReceived.await(10, TimeUnit.SECONDS), "No VERIFIER PRESENTATION_RECEIVED");
        
        /* 4. Acme verifies the Job Application Proof from Alice
         * 
         */
        
        presentationExchangeId = verifierExchangeRecord[0].getPresentationExchangeId();
        acme.presentProofRecordsVerifyPresentation(presentationExchangeId).get();
        
        Assertions.assertTrue(proverPresentationAcked.await(10, TimeUnit.SECONDS), "No PROVER PRESENTATION_ACKED");
        
        acmeSubscriber.cancelSubscription();
        aliceSubscriber.cancelSubscription();
    }

    void getJobWithAcme(Context ctx) throws Exception {

        logSection("Alice gets JobCertificate from Acme");
        
        WalletRecord acmeWallet = ctx.getWallet(Acme);
        String acmeAliceConnectionId = ctx.getConnection(Acme, Alice).getConnectionId();
        AriesClient acme = createClient(acmeWallet);
        
        WalletRecord aliceWallet = ctx.getWallet(Alice);
        AriesClient alice = createClient(aliceWallet);

        V1CredentialExchange[] issuerCredEx = new V1CredentialExchange[1];
        V1CredentialExchange[] holderCredEx = new V1CredentialExchange[1];
        CountDownLatch holderOfferReceived = new CountDownLatch(1);
        CountDownLatch issuerRequestReceived = new CountDownLatch(1);
        CountDownLatch holderCredentialReceived = new CountDownLatch(1);
        CountDownLatch holderCredentialAcked = new CountDownLatch(1);
        
        WebSocketEventHandler acmeHandler = WebSockets.getEventHandler(ctx.getWebSocket(Acme));
        EventSubscriber<WebSocketEvent> acmeSubscriber = acmeHandler.subscribe(V1CredentialExchange.class, ev -> { 
            V1CredentialExchange cex = ev.getPayload(V1CredentialExchange.class);
            log.info("{}: [@{}] {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), cex.getRole(), cex.getState(), cex); 
            if (CredentialExchangeRole.ISSUER == cex.getRole() && CredentialExchangeState.REQUEST_RECEIVED == cex.getState()) {
                issuerCredEx[0] = cex;
                issuerRequestReceived.countDown();
            }
        });
        
        WebSocketEventHandler aliceHandler = WebSockets.getEventHandler(ctx.getWebSocket(Alice));
        EventSubscriber<WebSocketEvent> aliceSubscriber = aliceHandler.subscribe(V1CredentialExchange.class, ev -> { 
            V1CredentialExchange cex = ev.getPayload(V1CredentialExchange.class);
            log.info("{}: [@{}] {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), cex.getRole(), cex.getState(), cex);
            if (CredentialExchangeRole.HOLDER == cex.getRole() && CredentialExchangeState.OFFER_RECEIVED == cex.getState()) {
                holderCredEx[0] = cex;
                holderOfferReceived.countDown();
            }
            else if (CredentialExchangeRole.HOLDER == cex.getRole() && CredentialExchangeState.CREDENTIAL_RECEIVED == cex.getState()) {
                holderCredEx[0] = cex;
                holderCredentialReceived.countDown();
            }
            else if (CredentialExchangeRole.HOLDER == cex.getRole() && CredentialExchangeState.CREDENTIAL_ACKED == cex.getState()) {
                holderCredEx[0] = cex;
                holderCredentialAcked.countDown();
            }
        });

        /* 1. Acme sends the JobCertificate Credential Offer
         * 
         * The value of this JobCertificate Credential is that it 
         * is verifiably proves that the holder is employed by Acme
         */
        
        String transcriptCredDefId = ctx.getIdentifier(JobCertificateCredDefId);
        acme.issueCredentialSendOffer(V1CredentialOfferRequest.builder()
                .connectionId(acmeAliceConnectionId)
                .credentialDefinitionId(transcriptCredDefId)
                .credentialPreview(new CredentialPreview(CredentialAttributes.from(Map.of(
                        "first_name", "Alice", 
                        "last_name", "Garcia", 
                        "employee_status", "Permanent", 
                        "experience", "10", 
                        "salary", "2400"))))
                .build()).get();
        
        /* 2. Alice inspects the the JobCertificate Credential Offer
         * 
         */
        
        Assertions.assertTrue(holderOfferReceived.await(10, TimeUnit.SECONDS), "No HOLDER OFFER_RECEIVED");
        
        CredentialProposal credentialProposal = holderCredEx[0].getCredentialProposalDict().getCredentialProposal();
        CredentialProposalHelper credentialHelper = new CredentialProposalHelper(credentialProposal);
        Assertions.assertEquals("Alice", credentialHelper.getAttributeValue("first_name"));
        Assertions.assertEquals("Garcia", credentialHelper.getAttributeValue("last_name"));
        Assertions.assertEquals("Permanent", credentialHelper.getAttributeValue("employee_status"));
        Assertions.assertEquals("2400", credentialHelper.getAttributeValue("salary"));
        
        /* 3. Alice sends the JobCertificate Credential Request
         * 
         */
        
        alice.issueCredentialRecordsSendRequest(holderCredEx[0].getCredentialExchangeId()).get();
        
        /* 4. Acme receives the JobCertificate Credential Request
         * 
         */

        Assertions.assertTrue(issuerRequestReceived.await(10, TimeUnit.SECONDS), "No ISSUER REQUEST_RECEIVED");
        
        /* 5. Acme issues the JobCertificate Credential
         * 
         */

        acme.issueCredentialRecordsIssue(issuerCredEx[0].getCredentialExchangeId(), V1CredentialIssueRequest.builder().build()).get();
        
        /* 6. Alice receives the Transcript Credential
         * 
         */

        Assertions.assertTrue(holderCredentialReceived.await(10, TimeUnit.SECONDS), "No HOLDER CREDENTIAL_RECEIVED");
        
        /* 7. Alice stores the Transcript Credential
         * 
         */

        alice.issueCredentialRecordsStore(holderCredEx[0].getCredentialExchangeId(), V1CredentialStoreRequest.builder()
                .credentialId(holderCredEx[0].getCredentialId())
                .build()).get();

        Assertions.assertTrue(holderCredentialAcked.await(10, TimeUnit.SECONDS), "No HOLDER CREDENTIAL_ACKED");
        
        acmeSubscriber.cancelSubscription();
        aliceSubscriber.cancelSubscription();
    }

    void applyForLoanWithThrift(Context ctx) throws Exception {
        
        logSection("Alice applies for a Loan with Thrift");
        
        WalletRecord thriftWallet = ctx.getWallet(Thrift);
        AriesClient thrift = createClient(thriftWallet);
        
        WalletRecord aliceWallet = ctx.getWallet(Alice);
        AriesClient alice = createClient(aliceWallet);
        
        PresentationExchangeRecord[] proverExchangeRecord = new PresentationExchangeRecord[1];
        PresentationExchangeRecord[] verifierExchangeRecord = new PresentationExchangeRecord[1];
        CountDownLatch proverRequestReceived = new CountDownLatch(1);
        CountDownLatch verifierPresentationReceived = new CountDownLatch(1);
        CountDownLatch proverPresentationAcked = new CountDownLatch(1);
        
        WebSocketEventHandler thriftHandler = WebSockets.getEventHandler(ctx.getWebSocket(Thrift));
        EventSubscriber<WebSocketEvent> thriftSubscriber = thriftHandler.subscribe(PresentationExchangeRecord.class, ev -> { 
            PresentationExchangeRecord pex = ev.getPayload(PresentationExchangeRecord.class);
            log.info("{}: [@{}] {} {} {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), pex.getRole(), pex.getState(), pex); 
            if (PresentationExchangeRole.VERIFIER == pex.getRole() && PresentationExchangeState.PRESENTATION_RECEIVED == pex.getState()) {
                verifierExchangeRecord[0] = pex;
                verifierPresentationReceived.countDown();
            }
        });
        
        WebSocketEventHandler aliceHandler = WebSockets.getEventHandler(ctx.getWebSocket(Alice));
        EventSubscriber<WebSocketEvent> aliceSubscriber = aliceHandler.subscribe(PresentationExchangeRecord.class, ev -> { 
            PresentationExchangeRecord pex = ev.getPayload(PresentationExchangeRecord.class);
            log.info("{}: [@{}] {} {} {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), pex.getRole(), pex.getState(), pex); 
            if (PresentationExchangeRole.PROVER == pex.getRole() && PresentationExchangeState.REQUEST_RECEIVED == pex.getState()) {
                proverExchangeRecord[0] = pex;
                proverRequestReceived.countDown();
            }
            if (PresentationExchangeRole.PROVER == pex.getRole() && PresentationExchangeState.PRESENTATION_ACKED == pex.getState()) {
                proverExchangeRecord[0] = pex;
                proverPresentationAcked.countDown();
            }
        });
       
        /* 1. Alice gets a Loan-Application Proof Request from Thrift Bank
         * 
         * Note, that the Job-Certificate should not have been revoked at the time of application.
         */
        
        String jobCertificateCredDefId = ctx.getIdentifier(JobCertificateCredDefId);
        String thriftAliceConnectionId = ctx.getConnection(Thrift, Alice).getConnectionId();
        
        Function<String, JsonObject> credDefRestriction = cdid -> gson.fromJson("{\"cred_def_id\"=\"" + cdid + "\"}", JsonObject.class);
        BiFunction<String, String, ProofRequestedAttributes> restrictedProofReqAttr = (name, cdid) -> ProofRequestedAttributes.builder()
                .name(name)
                .restriction(credDefRestriction.apply(cdid))
                .build();
        BiFunction<String, String, ProofRequestedPredicates> restrictedProofReqPred = (pred, cdid) -> ProofRequestedPredicates.builder()
                .name(pred.split(" ")[0])
                .pType(PTypeEnum.fromValue(pred.split(" ")[1]))
                .pValue(Integer.valueOf(pred.split(" ")[2]))
                .restriction(credDefRestriction.apply(cdid))
                .build();
        
        thrift.presentProofSendRequest(PresentProofRequest.builder()
                .connectionId(thriftAliceConnectionId)
                .proofRequest(ProofRequest.builder()
                        .name("Loan-Application")
                        .nonce("1")
                        .requestedAttribute("attr1_referent", restrictedProofReqAttr.apply("employee_status", jobCertificateCredDefId))
                        .requestedPredicate("pred1_referent", restrictedProofReqPred.apply("salary >= 2000", jobCertificateCredDefId))
                        .requestedPredicate("pred2_referent", restrictedProofReqPred.apply("experience >= 1", jobCertificateCredDefId))
                        .nonRevoked(ProofNonRevoked.builder()
                                .from(0L)
                                .to(Instant.now().getEpochSecond())
                                .build())
                        .build())
                .build()).get();
        
        Assertions.assertTrue(proverRequestReceived.await(10, TimeUnit.SECONDS), "No PROVER REQUEST_RECEIVED");
        
        // 2. Alice searches her Wallet for Credentials that she can use for the creating of Proof for the Loan-Application Proof Request
        
        Map<String, String> referentMapping = new HashMap<>();
        String presentationExchangeId = proverExchangeRecord[0].getPresentationExchangeId();
        alice.presentProofRecordsCredentials(presentationExchangeId).get().stream()
            .forEach(cred -> {
                List<String> presentationReferents = cred.getPresentationReferents();
                CredentialInfo credInfo = cred.getCredentialInfo();
                String credDefId = credInfo.getCredentialDefinitionId();
                Map<String, String> attributes = credInfo.getAttrs();
                String referent = credInfo.getReferent();
                log.debug("{}", cred); 
                log.debug("+- CredDefId: {}", credDefId); 
                log.debug("+- PresentationReferents: {}", presentationReferents); 
                log.debug("+- Attributes: {}", attributes); 
                log.debug("+- Referent: {}", referent); 
                
                // Map attribute referents to their respective credential referent
                presentationReferents.stream().forEach(pr -> referentMapping.put(pr, referent));
            });

        /* 3. Alice provides Loan-Application Proof
         * 
         * Alice divides these attributes into the three groups:
         * 
         * - attributes values of which will be revealed
         * - attributes values of which will be unrevealed
         * - attributes for which creating of verifiable proof is not required
         */
        
        BiFunction<String, Boolean, Map<String, IndyRequestedCredsRequestedAttr>> indyRequestedAttr = (ref, reveal) -> Map.of(ref, IndyRequestedCredsRequestedAttr.builder()
                .credId(referentMapping.get(ref))
                .revealed(reveal)
                .build());
        Function<String, Map<String, IndyRequestedCredsRequestedPred>> indyRequestedPred = ref -> Map.of(ref, IndyRequestedCredsRequestedPred.builder()
                .credId(referentMapping.get(ref))
                .build());
        
        alice.presentProofRecordsSendPresentation(presentationExchangeId, PresentationRequest.builder()
                .requestedAttributes(indyRequestedAttr.apply("attr1_referent", true))
                .requestedPredicates(indyRequestedPred.apply("pred1_referent"))
                .requestedPredicates(indyRequestedPred.apply("pred2_referent"))
                .build());

        Assertions.assertTrue(verifierPresentationReceived.await(10, TimeUnit.SECONDS), "No VERIFIER PRESENTATION_RECEIVED");
        
        /* 4. Thrift verifies the Loan-Application Proof from Alice
         * 
         */
        
        presentationExchangeId = verifierExchangeRecord[0].getPresentationExchangeId();
        thrift.presentProofRecordsVerifyPresentation(presentationExchangeId).get();
        
        Assertions.assertTrue(proverPresentationAcked.await(10, TimeUnit.SECONDS), "No PROVER PRESENTATION_ACKED");

        thriftSubscriber.cancelSubscription();
        aliceSubscriber.cancelSubscription();
    }

    void kycProcessWithThrift(Context ctx) throws Exception {

        logSection("Alice goes through the KYC process with Thrift");
        
        WalletRecord thriftWallet = ctx.getWallet(Thrift);
        AriesClient thrift = createClient(thriftWallet);
        
        WalletRecord aliceWallet = ctx.getWallet(Alice);
        AriesClient alice = createClient(aliceWallet);
        
        PresentationExchangeRecord[] proverExchangeRecord = new PresentationExchangeRecord[1];
        PresentationExchangeRecord[] verifierExchangeRecord = new PresentationExchangeRecord[1];
        CountDownLatch proverRequestReceived = new CountDownLatch(1);
        CountDownLatch verifierPresentationReceived = new CountDownLatch(1);
        CountDownLatch proverPresentationAcked = new CountDownLatch(1);
        
        WebSocketEventHandler thriftHandler = WebSockets.getEventHandler(ctx.getWebSocket(Thrift));
        EventSubscriber<WebSocketEvent> thriftSubscriber = thriftHandler.subscribe(PresentationExchangeRecord.class, ev -> { 
            PresentationExchangeRecord pex = ev.getPayload(PresentationExchangeRecord.class);
            log.info("{}: [@{}] {} {} {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), pex.getRole(), pex.getState(), pex); 
            if (PresentationExchangeRole.VERIFIER == pex.getRole() && PresentationExchangeState.PRESENTATION_RECEIVED == pex.getState()) {
                verifierExchangeRecord[0] = pex;
                verifierPresentationReceived.countDown();
            }
        });
        
        WebSocketEventHandler aliceHandler = WebSockets.getEventHandler(ctx.getWebSocket(Alice));
        EventSubscriber<WebSocketEvent> aliceSubscriber = aliceHandler.subscribe(PresentationExchangeRecord.class, ev -> { 
            PresentationExchangeRecord pex = ev.getPayload(PresentationExchangeRecord.class);
            log.info("{}: [@{}] {} {} {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), pex.getRole(), pex.getState(), pex); 
            if (PresentationExchangeRole.PROVER == pex.getRole() && PresentationExchangeState.REQUEST_RECEIVED == pex.getState()) {
                proverExchangeRecord[0] = pex;
                proverRequestReceived.countDown();
            }
            if (PresentationExchangeRole.PROVER == pex.getRole() && PresentationExchangeState.PRESENTATION_ACKED == pex.getState()) {
                proverExchangeRecord[0] = pex;
                proverPresentationAcked.countDown();
            }
        });
       
        /* 1. Alice gets a second Proof Request from Thrift Bank
         * 
         */
        
        String transcriptCredDefId = ctx.getIdentifier(TranscriptCredDefId);
        String thriftAliceConnectionId = ctx.getConnection(Thrift, Alice).getConnectionId();
        
        Function<String, JsonObject> credDefRestriction = cdid -> gson.fromJson("{\"cred_def_id\"=\"" + cdid + "\"}", JsonObject.class);
        BiFunction<String, String, ProofRequestedAttributes> restrictedProofReqAttr = (name, cdid) -> ProofRequestedAttributes.builder()
                .name(name)
                .restriction(credDefRestriction.apply(cdid))
                .build();
        
        thrift.presentProofSendRequest(PresentProofRequest.builder()
                .connectionId(thriftAliceConnectionId)
                .proofRequest(ProofRequest.builder()
                        .name("KYC-Application")
                        .nonce("1")
                        .requestedAttribute("attr1_referent", restrictedProofReqAttr.apply("first_name", transcriptCredDefId))
                        .requestedAttribute("attr2_referent", restrictedProofReqAttr.apply("last_name", transcriptCredDefId))
                        .requestedAttribute("attr3_referent", restrictedProofReqAttr.apply("ssn", transcriptCredDefId))
                        .build())
                .build()).get();
        
        Assertions.assertTrue(proverRequestReceived.await(10, TimeUnit.SECONDS), "No PROVER REQUEST_RECEIVED");
        
        // 2. Alice searches her Wallet for Credentials that she can use for the creating of Proof for the KYC Proof Request
        
        Map<String, String> referentMapping = new HashMap<>();
        String presentationExchangeId = proverExchangeRecord[0].getPresentationExchangeId();
        alice.presentProofRecordsCredentials(presentationExchangeId).get().stream()
            .forEach(cred -> {
                List<String> presentationReferents = cred.getPresentationReferents();
                CredentialInfo credInfo = cred.getCredentialInfo();
                String credDefId = credInfo.getCredentialDefinitionId();
                Map<String, String> attributes = credInfo.getAttrs();
                String referent = credInfo.getReferent();
                log.debug("{}", cred); 
                log.debug("+- CredDefId: {}", credDefId); 
                log.debug("+- PresentationReferents: {}", presentationReferents); 
                log.debug("+- Attributes: {}", attributes); 
                log.debug("+- Referent: {}", referent); 
                
                // Map attribute referents to their respective credential referent
                presentationReferents.stream().forEach(pr -> referentMapping.put(pr, referent));
            });

        /* 3. Alice provides KYC-Application Proof
         * 
         * Alice divides these attributes into the three groups:
         * 
         * - attributes values of which will be revealed
         * - attributes values of which will be unrevealed
         * - attributes for which creating of verifiable proof is not required
         */
        
        BiFunction<String, Boolean, Map<String, IndyRequestedCredsRequestedAttr>> indyRequestedAttr = (ref, reveal) -> Map.of(ref, IndyRequestedCredsRequestedAttr.builder()
                .credId(referentMapping.get(ref))
                .revealed(reveal)
                .build());
        
        alice.presentProofRecordsSendPresentation(presentationExchangeId, PresentationRequest.builder()
                .requestedAttributes(indyRequestedAttr.apply("attr1_referent", true))
                .requestedAttributes(indyRequestedAttr.apply("attr2_referent", true))
                .requestedAttributes(indyRequestedAttr.apply("attr3_referent", true))
                .build());

        Assertions.assertTrue(verifierPresentationReceived.await(10, TimeUnit.SECONDS), "No VERIFIER PRESENTATION_RECEIVED");
        
        /* 4. Thrift verifies the KYC-Application Proof from Alice
         * 
         */
        
        presentationExchangeId = verifierExchangeRecord[0].getPresentationExchangeId();
        thrift.presentProofRecordsVerifyPresentation(presentationExchangeId).get();
        
        Assertions.assertTrue(proverPresentationAcked.await(10, TimeUnit.SECONDS), "No PROVER PRESENTATION_ACKED");

        thriftSubscriber.cancelSubscription();
        aliceSubscriber.cancelSubscription();
    }

    void acmeRevokesTheJobCertificate(Context ctx) throws Exception {
        
        logSection("Acme revokes the Job-Certificate Credential");

        WalletRecord acmeWallet = ctx.getWallet(Acme);
        String acmeWalletId = acmeWallet.getWalletId();
        
        CountDownLatch revocationEventLatch = new CountDownLatch(1);
        CountDownLatch credentialRevokedLatch = new CountDownLatch(1);
        
        WebSocketEventHandler acmeHandler = WebSockets.getEventHandler(ctx.getWebSocket(Acme));
        EventSubscriber<WebSocketEvent> acmeSubscriber = acmeHandler.subscribe(RevocationEvent.class, ev -> { 
            RevocationEvent revoc = ev.getPayload(RevocationEvent.class);
            log.info("{}: [@{}] {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), revoc.getState(), revoc); 
            if ("revoked".equals(revoc.getState())) {
                revocationEventLatch.countDown();
            }
        });
        
        WebSocketEventHandler aliceHandler = WebSockets.getEventHandler(ctx.getWebSocket(Alice));
        EventSubscriber<WebSocketEvent> aliceSubscriber = aliceHandler.subscribeFromOther(acmeWalletId, V1CredentialExchange.class, ev -> { 
            V1CredentialExchange cex = ev.getPayload(V1CredentialExchange.class);
            log.info("{}: [@{}] {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), cex.getRole(), cex.getState(), cex); 
            if (CredentialExchangeRole.ISSUER == cex.getRole() && CredentialExchangeState.CREDENTIAL_REVOKED == cex.getState()) {
                credentialRevokedLatch.countDown();
            }
        });
        
        // 1. Acme searches the Job-Certificate Credential
        
        AriesClient acme = createClient(acmeWallet);
        
        String connectionId = ctx.getConnection(Acme, Alice).getConnectionId();
        String jobCertificateCredDefId = ctx.getIdentifier(JobCertificateCredDefId);
        Predicate<String> matchCredDefId = cdid -> cdid.equals(jobCertificateCredDefId);
        V1CredentialExchange credex = acme.issueCredentialRecords(IssueCredentialRecordsFilter.builder()
                .connectionId(connectionId)
                .build()).get().stream()
                    .filter(cr -> matchCredDefId.test(cr.getCredentialDefinitionId()))
                    .findFirst().get();
        
        // 2. Acme revokes the Job-Certificate Credential
        
        acme.revocationRevoke(RevokeRequest.builder()
                .credExId(credex.getCredentialExchangeId())
                .connectionId(connectionId)
                .publish(true)
                .notify(true)
                .build()).get();
        
        Assertions.assertTrue(revocationEventLatch.await(10, TimeUnit.SECONDS), "No RevocationEvent");
        Assertions.assertTrue(credentialRevokedLatch.await(10, TimeUnit.SECONDS), "No ISSUER CREDENTIAL_REVOKED");
        
        aliceSubscriber.cancelSubscription();
        acmeSubscriber.cancelSubscription();
    }

    void closeAndDeleteWallets(Context ctx) throws Exception {
        logSection("Remove Wallets");
        for (String name : Arrays.asList(Government, Faber, Acme, Thrift, Alice)) {
            closeWebSocket(ctx.getWebSocket(name));
            removeWallet(ctx.getWallet(name));
        }
    }
}
