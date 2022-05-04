package org.apache.camel.component.aries;

import static org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole.ISSUER;
import static org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState.OFFER_SENT;
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialFreeOfferRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * docker compose up --detach && docker compose logs -f acapy
 */
public class CredentialCreateOfferTest extends AbstractHyperledgerAriesTest {

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
                    .to("seda:transscript-credential-offer");
                
                // Faber sends the Transcript Credential Offer
                from("seda:transscript-credential-offer")
                    .setBody(ex -> V1CredentialFreeOfferRequest.builder()
                            .credDefId(ex.getIn().getHeader("CredentialDefinitionId", String.class))
                            .credentialPreview(new CredentialPreview(CredentialAttributes.from(Map.of(
                                    "first_name", "Alice", 
                                    "last_name", "Garcia", 
                                    "ssn", "123-45-6789", 
                                    "degree", "Bachelor of Science, Marketing", 
                                    "status", "graduated", 
                                    "year", "2015", 
                                    "average", "5"))))
                            .build())
                    .to("hyperledger-aries:faber?service=/issue-credential/create-offer");
            }
        };
    }
    
    @Test
    public void testWorkflow() throws Exception {
        
        getComponent().setRemoveWalletsOnShutdown(true);
        
        onboardWallet(Faber, ENDORSER);
        
        Map<String, Object> reqBody = Map.of(
                "schemaVersion", "1.2",
                "attributes", "first_name, last_name, ssn, degree, status, year, average",
                "supportRevocation", "false"
            );

        V1CredentialExchange resObj = template.requestBody("direct:transscript-credential-definition", reqBody, V1CredentialExchange.class);
        Assertions.assertEquals(ISSUER, resObj.getRole());
        Assertions.assertEquals(OFFER_SENT, resObj.getState());
    }
}
