package org.apache.camel.component.aries.handler;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.apache.camel.component.aries.UnsupportedServiceException;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionResponse;

public class CredentialDefinitionsServiceHandler extends AbstractServiceHandler {
    
    public CredentialDefinitionsServiceHandler(HyperledgerAriesEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange, String service) throws Exception {
        if (service.equals("/credential-definitions")) {
            CredentialDefinitionRequest reqObj = assertBody(exchange, CredentialDefinitionRequest.class);
            CredentialDefinitionResponse resObj = createClient().credentialDefinitionsCreate(reqObj).get();
            exchange.getIn().setBody(resObj);
        }
        else throw new UnsupportedServiceException(service);
    }
}