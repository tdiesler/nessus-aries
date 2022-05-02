package org.apache.camel.component.aries.handler;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesComponent;
import org.apache.camel.component.aries.HyperledgerAriesConfiguration;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.aries.util.AssertState;

public abstract class AbstractServiceHandler implements ServiceHandler {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final HyperledgerAriesEndpoint endpoint;
    
    public AbstractServiceHandler(HyperledgerAriesEndpoint endpoint) {
        this.endpoint = endpoint;
    }
    
    protected String getServicePathToken(String service, int idx) {
        return service.split("/")[idx + 1];
    }
    
    public <T> T assertBody(Exchange exchange, Class<T> type) {
        T body = exchange.getIn().getBody(type);
        AssertState.notNull(body, "Cannot obtain body of type: " + type.getName());
        return body;
    }

    public boolean hasHeader(Exchange exchange, String key) {
        return exchange.getIn().getHeader(key) != null;
    }
    
    public <T> T getHeader(Exchange exchange, Class<T> type) {
        return getHeader(exchange, type.getName(), type);
    }
    
    public <T> T assertHeader(Exchange exchange, Class<T> type) {
        return assertHeader(exchange, type.getName(), type);
    }

    public <T> T getHeader(Exchange exchange, String key, Class<T> type) {
        return exchange.getIn().getHeader(key, type);
    }
    
    public <T> T assertHeader(Exchange exchange, String key, Class<T> type) {
        T val = getHeader(exchange, key, type);
        AssertState.notNull(val, "Cannot obtain header '" + key + "' of type: " + type.getName());
        return val;
    }

    public HyperledgerAriesConfiguration getConfiguration() {
        return endpoint.getConfiguration();
    }
    
    public HyperledgerAriesComponent getComponent() {
        return endpoint.getComponent();
    }
    
    public AriesClient baseClient() {
        return getComponent().baseClient();
    }
    
    public AriesClient createClient() throws IOException {
        return endpoint.createClient();
    }
    
    public AriesClient createClient(WalletRecord walletRecord) throws IOException {
        return getComponent().createClient(walletRecord);
    }
}