package org.apache.camel.component.aries.handler;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.apache.camel.component.aries.UnsupportedServiceException;
import org.hyperledger.acy_py.generated.model.DID;

public class WalletServiceHandler extends AbstractServiceHandler {
    
    public WalletServiceHandler(HyperledgerAriesEndpoint endpoint, String service) {
        super(endpoint, service);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        if (service.equals("/wallet/did/public")) {
            DID resObj = createClient().walletDidPublic().orElse(null);
            exchange.getIn().setBody(resObj);
        }
        else throw new UnsupportedServiceException(service);
    }
}