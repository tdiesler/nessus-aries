package org.apache.camel.component.aries.handler;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.apache.camel.component.aries.UnsupportedServiceException;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionResponse;

public class CredentialDefinitionsServiceHandler extends AbstractServiceHandler {
    
    public CredentialDefinitionsServiceHandler(HyperledgerAriesEndpoint endpoint, String service) {
        super(endpoint, service);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        if (service.equals("/credential-definitions")) {
            CredentialDefinitionRequest reqObj = assertBody(exchange, CredentialDefinitionRequest.class);
            CredentialDefinitionResponse resObj = createClient().credentialDefinitionsCreate(reqObj).get();
            exchange.getIn().setBody(resObj);
        }
        else throw new UnsupportedServiceException(service);
    }
}