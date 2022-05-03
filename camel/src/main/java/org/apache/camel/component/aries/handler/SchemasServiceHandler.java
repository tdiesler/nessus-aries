package org.apache.camel.component.aries.handler;

import java.util.Arrays;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.apache.camel.component.aries.UnsupportedServiceException;
import org.hyperledger.aries.api.schema.SchemaSendRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;

public class SchemasServiceHandler extends AbstractServiceHandler {
    
    public SchemasServiceHandler(HyperledgerAriesEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange, String service) throws Exception {
        if (service.equals("/schemas")) {
            SchemaSendRequest schemaReq = getBody(exchange, SchemaSendRequest.class);
            if (schemaReq == null) {
                Map<String, String> spec = assertBody(exchange, Map.class);
                String schemaName = spec.get("schemaName");
                if (schemaName == null)
                    schemaName = endpoint.getConfiguration().getSchemaName();
                String schemaVersion = spec.get("schemaVersion");
                String[] attributes = spec.get("attributes").split(",\\s*");
                schemaReq = SchemaSendRequest.builder()
                        .schemaName(schemaName)
                        .schemaVersion(schemaVersion)
                        .attributes(Arrays.asList(attributes))
                        .build();
            }
            SchemaSendResponse resObj = createClient().schemas(schemaReq).get();
            exchange.getIn().setBody(resObj);
        }
        else throw new UnsupportedServiceException(service);
    }
}