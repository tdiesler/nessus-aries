package org.apache.camel.component.aries.handler;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.apache.camel.component.aries.UnsupportedServiceException;
import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;

public class WalletServiceHandler extends AbstractServiceHandler {
    
    public WalletServiceHandler(HyperledgerAriesEndpoint endpoint, String service) {
        super(endpoint, service);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        if (service.contains("/did/public")) {
            AriesClient client = createClient();
            DID did = client.walletDidPublic().orElse(null);
            exchange.getIn().setBody(did);
        }
        else throw new UnsupportedServiceException(service);
    }
}