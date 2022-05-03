package org.apache.camel.component.aries;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * docker compose up --detach && docker compose logs -f acapy
 */
public class SchemaSendTest extends AbstractHyperledgerAriesTest {

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                
                from("direct:transscript-schema-send")
                
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
        
        onboardWallet(Faber, ENDORSER);
        
        // Faber creates the Transcript Schema and sends it to the Ledger
        // It can do so with it's Endorser role

        Map<String, String> schemaSpec = Map.of(
            "attributes", "first_name, last_name, ssn, degree, status, year, average",
            "schemaVersion", "1.2"
        );
        
        String schemaId = template.requestBody("direct:transscript-schema-send", schemaSpec, String.class);
        log.info("{}", schemaId);
        
        Assertions.assertTrue(schemaId.endsWith(":Transscript:1.2"));
    }
}
