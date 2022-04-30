/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.aries;

import static org.apache.camel.component.aries.Constants.HEADER_MULTITENANCY_WALLET_REGISTER_NYM;
import static org.apache.camel.component.aries.Constants.HEADER_MULTITENANCY_WALLET_ROLE;
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.TRUSTEE;

import java.io.IOException;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.multitenancy.CreateWalletRequest;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nessus.aries.util.AttachmentKey;
import io.nessus.aries.util.AttachmentSupport;
import okhttp3.WebSocket;

//@EnabledIfSystemProperty(named = "enable.aries.itests", matches = "true", disabledReason = "Requires API credentials")
public class GettingStartedCamelTest extends AbstractHyperledgerAriesTest {

    static final String Government = "Government";
    static final String Faber = "Faber";
    static final String Acme = "Acme";
    static final String Thrift = "Thrift";
    static final String Alice = "Alice";
    
    static final String TranscriptSchemaId = "TranscriptSchemaId";
    static final String TranscriptCredDefId = "TranscriptSchemaId";
    static final String JobCertificateSchemaId = "JobCertificateSchemaId";
    static final String JobCertificateCredDefId = "JobCertificateCredDefId";
    
    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                
                from("direct:admin")
                    .to("hyperledger-aries:admin?service=/multitenancy/wallet");
                
                from("direct:government")
                    .to("hyperledger-aries:government");
                
                from("direct:faber")
                    .to("hyperledger-aries:faber");
                
                from("direct:acme")
                    .to("hyperledger-aries:acme");
                
                from("direct:thrift")
                    .to("hyperledger-aries:thrift");
                
                from("direct:alice")
                    .to("hyperledger-aries:alice");
            }
        };
    }

    class WalktroughContext extends AttachmentSupport {

        ConnectionRecord getConnection(String inviter, String invitee) {
            return getAttachment(inviter + invitee + "Connection", ConnectionRecord.class);
        }

        String getStringIdentifier(String name) {
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
    void testWalktrough() throws Exception {

        WalktroughContext ctx = new WalktroughContext();
        getComponent().setRemoveWalletsOnShutdown(true);
        
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

//        createTranscriptSchema(ctx);
//        createJobCertificateSchema(ctx);

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

//        createTranscriptCredentialDefinition(ctx);
//        createJobCertificateCredentialDefinition(ctx);

        /*
         * Create a peer connection between Alice/Faber
         * 
         * Alice does not connect to Faber's public DID, Alice does not even have a public DID
         * Instead both parties create new DIDs that they use for their peer connection 
         */
        
//        connectPeers(ctx, Faber, Alice);

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

//        getTranscriptFromFaber(ctx);

        /*
         * Create a peer connection between Alice/Acme
         * 
         *  Alice does not connect to Faber's public DID, Alice does not even have a public DID
         *  Instead both parties create new DIDs that they use for their peer connection 
         */
         
//        connectPeers(ctx, Acme, Alice);

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

//        applyForJobWithAcme(ctx);

        /*
         * Alice gets the job and hence receives a JobCertificate from Acme
         * 
         * This is similar to the Transcript VC that she got from Faber, except that the 
         * JobCertificate credential can be revoked by Acme  
         */
        
//        getJobWithAcme(ctx); 

        /*
         * Create a peer connection between Alice/Faber
         * 
         * Alice does not connect to Faber's public DID, Alice does not even have a public DID
         * Instead both parties create new DIDs that they use for their peer connection 
         */
         
//        connectPeers(ctx, Thrift, Alice);

        /*
         * Alice applies for a loan with Thrift Bank
         * 
         * Now that Alice has a job, she’d like to apply for a loan. That will require a
         * proof of employment. She can get this from the Job-Certificate credential
         * offered by Acme.
         */

//        applyForLoanWithThrift(ctx, true);

        /*
         * Thrift accepts the loan application and now requires KYC
         * 
         * Thrift Bank sends the second Proof Request where Alice needs to share her
         * personal information with the bank.
         */

//        kycProcessWithThrift(ctx);

        /*
         * Alice decides to quit her job with Acme
         */

//        acmeRevokesTheJobCertificate(ctx);

        /*
         * Alice applies for another loan with Thrift Bank - this time without having a Job
         * 
         */
        
//        applyForLoanWithThrift(ctx, false);
    }

    void onboardGovernment(WalktroughContext ctx) throws IOException {

        String walletName = Government;
        logSection("Onboard " + walletName);
        
        CreateWalletRequest walletRequest = CreateWalletRequest.builder()
                .walletKey(walletName + "Key")
                .walletName(walletName)
                .build();

        WalletRecord wallet = template.requestBodyAndHeaders("direct:admin", walletRequest, Map.of(
                HEADER_MULTITENANCY_WALLET_REGISTER_NYM, true, 
                HEADER_MULTITENANCY_WALLET_ROLE, TRUSTEE),
                WalletRecord.class);
        
        Assertions.assertNotNull(wallet, "Wallet not null");
    }

    void onboardFaberCollege(WalktroughContext ctx) {

        String walletName = Faber;
        logSection("Onboard " + walletName);
        
        CreateWalletRequest walletRequest = CreateWalletRequest.builder()
                .walletKey(walletName + "Key")
                .walletName(walletName)
                .build();

        WalletRecord wallet = template.requestBodyAndHeaders("direct:admin", walletRequest, Map.of(
                HEADER_MULTITENANCY_WALLET_REGISTER_NYM, true, 
                HEADER_MULTITENANCY_WALLET_ROLE, ENDORSER),
                WalletRecord.class);
        
        Assertions.assertNotNull(wallet, "Wallet not null");
    }

    void onboardAcmeCorp(WalktroughContext ctx) {

        String walletName = Acme;
        logSection("Onboard " + walletName);
        
        CreateWalletRequest walletRequest = CreateWalletRequest.builder()
                .walletKey(walletName + "Key")
                .walletName(walletName)
                .build();

        WalletRecord wallet = template.requestBodyAndHeaders("direct:admin", walletRequest, Map.of(
                HEADER_MULTITENANCY_WALLET_REGISTER_NYM, true, 
                HEADER_MULTITENANCY_WALLET_ROLE, ENDORSER),
                WalletRecord.class);
        
        Assertions.assertNotNull(wallet, "Wallet not null");
    }

    void onboardThriftBank(WalktroughContext ctx) {

        String walletName = Thrift;
        logSection("Onboard " + walletName);
        
        CreateWalletRequest walletRequest = CreateWalletRequest.builder()
                .walletKey(walletName + "Key")
                .walletName(walletName)
                .build();

        WalletRecord wallet = template.requestBodyAndHeaders("direct:admin", walletRequest, Map.of(
                HEADER_MULTITENANCY_WALLET_REGISTER_NYM, true, 
                HEADER_MULTITENANCY_WALLET_ROLE, ENDORSER),
                WalletRecord.class);
        
        Assertions.assertNotNull(wallet, "Wallet not null");
    }

    void onboardAlice(WalktroughContext ctx) {

        String walletName = Alice;
        logSection("Onboard " + walletName);
        
        CreateWalletRequest walletRequest = CreateWalletRequest.builder()
                .walletKey(walletName + "Key")
                .walletName(walletName)
                .build();

        WalletRecord wallet = template.requestBody("direct:admin", walletRequest, WalletRecord.class);
        
        Assertions.assertNotNull(wallet, "Wallet not null");
    }

    void createTranscriptSchema(WalktroughContext ctx) {
    }

    void createJobCertificateSchema(WalktroughContext ctx) {
    }

    void createTranscriptCredentialDefinition(WalktroughContext ctx) {
    }

    void createJobCertificateCredentialDefinition(WalktroughContext ctx) {
    }

    void getTranscriptFromFaber(WalktroughContext ctx) {
    }

    void applyForJobWithAcme(WalktroughContext ctx) {
    }

    void getJobWithAcme(WalktroughContext ctx) {
    }

    void connectPeers(WalktroughContext ctx, String thrift2, String alice2) {
    }

    void kycProcessWithThrift(WalktroughContext ctx) {
    }

    void acmeRevokesTheJobCertificate(WalktroughContext ctx) {
    }

    void applyForLoanWithThrift(WalktroughContext ctx, boolean b) {
    }
}
