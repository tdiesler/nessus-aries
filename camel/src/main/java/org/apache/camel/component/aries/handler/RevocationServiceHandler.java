package org.apache.camel.component.aries.handler;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.apache.camel.component.aries.UnsupportedServiceException;
import org.hyperledger.acy_py.generated.model.IssuerRevRegRecord;
import org.hyperledger.aries.api.revocation.RevRegCreateRequest;
import org.hyperledger.aries.api.revocation.RevRegCreateResponse.RevocationModuleResponse;
import org.hyperledger.aries.api.revocation.RevokeRequest;

public class RevocationServiceHandler extends AbstractServiceHandler {
    
    public RevocationServiceHandler(HyperledgerAriesEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange, String service) throws Exception {
        if (service.equals("/revocation/create-registry")) {
            RevRegCreateRequest reqObj = assertBody(exchange, RevRegCreateRequest.class);
            IssuerRevRegRecord resObj = createClient().revocationCreateRegistry(reqObj).get();
            exchange.getIn().setBody(resObj);
        }
        else if (service.equals("/revocation/revoke")) {
            RevokeRequest reqObj = assertBody(exchange, RevokeRequest.class);
            RevocationModuleResponse resObj = createClient().revocationRevoke(reqObj).get();
            exchange.getIn().setBody(resObj);
        }
        else throw new UnsupportedServiceException(service);
    }
}