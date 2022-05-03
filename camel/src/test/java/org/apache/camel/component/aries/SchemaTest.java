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
public class SchemaTest extends AbstractHyperledgerAriesTest {

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                
                from("direct:transscript-schema")
                
                    // Faber creates a Transscript Schema
                    .to("hyperledger-aries:faber?service=/schemas&schemaName=Transscript")
                    
                    // Transform SchemaSendResponse => schemaId
                    .transform(simple("${body.schemaId}"));
            }
        };
    }

    @Test
    public void testWorkflow() throws Exception {
        
        getComponent().setRemoveWalletsOnShutdown(true);
        
        NessusWallet faberWallet = onboardWallet(Faber, ENDORSER);
        DID publicDid = faberWallet.getPublicDid();
        Assertions.assertNotNull(publicDid, "No public DID");
        
        // Faber creates the Transcript Schema and sends it to the Ledger
        // It can do so with it's Endorser role

        Map<String, String> schemaSpec = Map.of(
            "attributes", "first_name, last_name, ssn, degree, status, year, average",
            "schemaVersion", "1.2"
        );
        
        String schemaId = template.requestBody("direct:transscript-schema", schemaSpec, String.class);
        log.info("{}", schemaId);
        
        Assertions.assertTrue(schemaId.startsWith(publicDid.getDid()));
        Assertions.assertTrue(schemaId.endsWith(":Transscript:1.2"));
    }
}
