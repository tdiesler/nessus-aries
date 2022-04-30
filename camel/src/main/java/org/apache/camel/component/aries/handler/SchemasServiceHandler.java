package org.apache.camel.component.aries.handler;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.apache.camel.component.aries.UnsupportedServiceException;
import org.hyperledger.aries.api.schema.SchemaSendRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;

public class SchemasServiceHandler extends AbstractServiceHandler {
    
    public SchemasServiceHandler(HyperledgerAriesEndpoint endpoint, String service) {
        super(endpoint, service);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        if (service.equals("/schemas")) {
            SchemaSendRequest reqObj = assertBody(exchange, SchemaSendRequest.class);
            SchemaSendResponse resObj = createClient().schemas(reqObj).get();
            exchange.getIn().setBody(resObj);
        }
        else throw new UnsupportedServiceException(service);
    }
}