package org.apache.camel.component.aries;

import static org.apache.camel.component.aries.processor.CredentialExchangeEventProcessor.awaitHolderCredentialAcked;
import static org.apache.camel.component.aries.processor.CredentialExchangeEventProcessor.awaitHolderCredentialReceived;
import static org.apache.camel.component.aries.processor.CredentialExchangeEventProcessor.awaitHolderOfferReceived;
import static org.apache.camel.component.aries.processor.CredentialExchangeEventProcessor.awaitIssuerRequestReceived;
import static org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole.HOLDER;
import static org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole.ISSUER;
import static org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState.CREDENTIAL_ACKED;
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aries.processor.ConnectionEventProcessor;
import org.apache.camel.component.aries.processor.ConnectionResultProcessor;
import org.apache.camel.component.aries.processor.CredentialExchangeEventProcessor;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialOfferRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * docker compose up --detach && docker compose logs -f acapy
 */
public class CredentialOfferRequestIssueTest extends AbstractHyperledgerAriesTest {

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                
                // Faber creates a Transscript Credential Definition
                // Note, the Schema is created on-demand
                from("direct:transscript-credential-definition")
                    .to("hyperledger-aries:faber?service=/credential-definitions&schemaName=Transscript&autoSchema=true")
                    .setHeader("CredentialDefinitionId", simple("${body.credentialDefinitionId}"))
                    .to("seda:faber-alice-connect");
                
                // Faber connects to Alice
                from("seda:faber-alice-connect")
                    .to("hyperledger-aries:faber?service=/connections/create-invitation")
                    .transform(simple("${body.invitation}"))
                    .setHeader("ConnectionReceiveInvitationFilter", () -> Map.of("auto_accept", true))
                    .process(new ConnectionEventProcessor(Faber))
                    .process(new ConnectionEventProcessor(Alice))
                    .to("hyperledger-aries:alice?service=/connections/receive-invitation")
                    .process(new ConnectionResultProcessor(Faber, Alice))
                    .to("seda:transscript-credential-offer");
                
                // Faber sends the Transcript Credential Offer
                from("seda:transscript-credential-offer")
                    .setBody(ex -> V1CredentialOfferRequest.builder()
                            .connectionId(ex.getIn().getHeader("FaberAliceConnectionRecord", ConnectionRecord.class).getConnectionId())
                            .credentialDefinitionId(ex.getIn().getHeader("CredentialDefinitionId", String.class))
                            .credentialPreview(new CredentialPreview(CredentialAttributes.from(Map.of(
                                    "first_name", "Alice", 
                                    "last_name", "Garcia", 
                                    "ssn", "123-45-6789", 
                                    "degree", "Bachelor of Science, Marketing", 
                                    "status", "graduated", 
                                    "year", "2015", 
                                    "average", "5"))))
                            .build())
                    .process(new CredentialExchangeEventProcessor(Faber, ISSUER))
                    .process(new CredentialExchangeEventProcessor(Alice, HOLDER))
                    .to("hyperledger-aries:faber?service=/issue-credential/send-offer")
                    .to("seda:send-credential-request");
                
                // Faber issues the Transcript Credential
                from("seda:send-credential-request")
                    .process(ex -> awaitHolderOfferReceived(ex, 10, TimeUnit.SECONDS))
                    .toD("hyperledger-aries:alice?service=/issue-credential/records/${header.holderCredentialExchange.credentialExchangeId}/send-request")
                    .process(ex -> awaitIssuerRequestReceived(ex, 10, TimeUnit.SECONDS))
                    .toD("hyperledger-aries:faber?service=/issue-credential/records/${header.issuerCredentialExchange.credentialExchangeId}/issue")
                    .process(ex -> awaitHolderCredentialReceived(ex, 10, TimeUnit.SECONDS))
                    .toD("hyperledger-aries:alice?service=/issue-credential/records/${header.holderCredentialExchange.credentialExchangeId}/store")
                    .process(ex -> awaitHolderCredentialAcked(ex, 10, TimeUnit.SECONDS));
            }
        };
    }
    
    @Test
    public void testWorkflow() throws Exception {
        
        getComponent().setRemoveWalletsOnShutdown(true);
        
        onboardWallet(Faber, ENDORSER);
        onboardWallet(Alice);
        
        Map<String, Object> reqSpec = Map.of(
                "schemaVersion", "1.2",
                "attributes", "first_name, last_name, ssn, degree, status, year, average",
                "supportRevocation", "false"
            );

        V1CredentialExchange resObj = template.requestBody("direct:transscript-credential-definition", reqSpec, V1CredentialExchange.class);
        Assertions.assertEquals(HOLDER, resObj.getRole());
        Assertions.assertEquals(CREDENTIAL_ACKED, resObj.getState());
    }
}
