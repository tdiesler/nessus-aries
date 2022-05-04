package org.apache.camel.component.aries;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.hyperledger.acy_py.generated.model.DID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nessus.aries.wallet.NessusWallet;

/**
 * docker compose up --detach && docker compose logs -f acapy
 */
public class CredentialDefinitionTest extends AbstractHyperledgerAriesTest {

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                
                // Faber creates a Transscript Credential Definition
                // Note, the Schema is created on-demand
                from("direct:transscript-credential-definition")
                    .to("hyperledger-aries:faber?service=/credential-definitions&schemaName=Transscript&autoSchema=true")
                    .transform(simple("${body.credentialDefinitionId}"))
                    .log("credentialDefinitionId: ${body}");
            }
        };
    }

    @Test
    public void testWorkflow() throws Exception {
        
        getComponent().setRemoveWalletsOnShutdown(true);
        
        NessusWallet faberWallet = onboardWallet(Faber, ENDORSER);
        DID publicDid = faberWallet.getPublicDid();
        
        Map<String, Object> reqBody = Map.of(
                "schemaVersion", "1.2",
                "attributes", "first_name, last_name, ssn, degree, status, year, average",
                "supportRevocation", "false"
            );

        String credDefId = template.requestBody("direct:transscript-credential-definition", reqBody, String.class);
        Assertions.assertTrue(credDefId.startsWith(publicDid.getDid()));
    }
}
