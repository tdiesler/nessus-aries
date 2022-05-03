package org.apache.camel.component.aries.handler;

import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.apache.camel.component.aries.UnsupportedServiceException;
import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionResponse;
import org.hyperledger.aries.api.schema.SchemasCreatedFilter;

import io.nessus.aries.util.AssertState;

public class CredentialDefinitionsServiceHandler extends AbstractServiceHandler {
    
    public CredentialDefinitionsServiceHandler(HyperledgerAriesEndpoint endpoint) {
        super(endpoint);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(Exchange exchange, String service) throws Exception {
        if (service.equals("/credential-definitions")) {
            CredentialDefinitionRequest credDefReq = getBody(exchange, CredentialDefinitionRequest.class);
            if (credDefReq == null) {
                Map<String, String> spec = assertBody(exchange, Map.class);
                String schemaName = spec.get("schemaName");
                if (schemaName == null)
                    schemaName = endpoint.getConfiguration().getSchemaName();
                AssertState.notNull(schemaName, "Cannot obtain schemaName");
                String schemaVersion = spec.get("schemaVersion");
                if (schemaVersion == null)
                    schemaVersion = endpoint.getConfiguration().getSchemaVersion();
                AssertState.notNull(schemaVersion, "Cannot obtain schemaVersion");
                Boolean supportRevocation = Boolean.valueOf(spec.get("supportRevocation"));
                
                DID publicDid = createClient().walletDidPublic().get();
                List<String> schemaIds = createClient().schemasCreated(SchemasCreatedFilter.builder()
                        .schemaIssuerDid(publicDid.getDid())
                        .schemaName(schemaName)
                        .schemaVersion(schemaVersion)
                        .build()).get();
                AssertState.isEqual(1, schemaIds.size(), "Unexpected number of schema ids: " + schemaIds);
                credDefReq = CredentialDefinitionRequest.builder()
                        .supportRevocation(supportRevocation)
                        .schemaId(schemaIds.get(0))
                        .build();
            }
            CredentialDefinitionResponse resObj = createClient().credentialDefinitionsCreate(credDefReq).get();
            exchange.getIn().setBody(resObj);
        }
        else throw new UnsupportedServiceException(service);
    }
}