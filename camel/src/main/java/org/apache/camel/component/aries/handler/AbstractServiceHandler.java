package org.apache.camel.component.aries.handler;

import static org.apache.camel.component.aries.Constants.HEADER_NESSUS_WALLET;
import static org.apache.camel.component.aries.Constants.HEADER_WALLET_NAME;
import static org.apache.camel.component.aries.Constants.PROPERTY_HYPERLEDGER_ARIES_COMPONENT;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.aries.HyperledgerAriesComponent;
import org.apache.camel.component.aries.HyperledgerAriesConfiguration;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.hyperledger.aries.AriesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.aries.util.AssertState;
import io.nessus.aries.wallet.NessusWallet;

public abstract class AbstractServiceHandler implements ServiceHandler {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final HyperledgerAriesEndpoint endpoint;
    
    public AbstractServiceHandler(HyperledgerAriesEndpoint endpoint) {
        this.endpoint = endpoint;
    }
    
    public void beforeProcess(Exchange exchange, String service) {
        String walletName = endpoint.getWalletName();
        exchange.setProperty(PROPERTY_HYPERLEDGER_ARIES_COMPONENT, endpoint.getComponent());
        Message message = exchange.getIn();
        message.setHeader(HEADER_WALLET_NAME, walletName);
        message.setHeader(HEADER_NESSUS_WALLET, endpoint.getWallet());
        log.info("{}: Before [service={}, body={}, headers={}]", walletName, service, message.getBody(), message.getHeaders());
    }
    
    public void afterProcess(Exchange exchange, String service) {
        Message message = exchange.getIn();
        String walletName = endpoint.getWalletName();
        log.info("{}: After [service={}, body={}, headers={}]", walletName, service, message.getBody(), message.getHeaders());
    }

    protected String getServicePathToken(String service, int idx) {
        return service.split("/")[idx + 1];
    }
    
    public <T> boolean hasBody(Exchange exchange, Class<T> type) {
        return getBody(exchange, type) != null;
    }

    public <T> T getBody(Exchange exchange, Class<T> type) {
        return exchange.getIn().getBody(type);
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
        T value = getHeader(exchange, type.getSimpleName(), type);
        if (value == null) 
            value = getHeader(exchange, type.getName(), type);
        return value;
    }
    
    public <T> T assertHeader(Exchange exchange, Class<T> type) {
        T value = getHeader(exchange, type);
        AssertState.notNull(value, "Cannot obtain header of type: " + type.getName());
        return value;
    }

    public <T> T getHeader(Exchange exchange, String key, Class<T> type) {
        return exchange.getIn().getHeader(key, type);
    }
    
    public <T> T assertHeader(Exchange exchange, String key, Class<T> type) {
        T value = getHeader(exchange, key, type);
        AssertState.notNull(value, "Cannot obtain header '" + key + "' of type: " + type.getName());
        return value;
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
    
    public AriesClient createClient(NessusWallet walletRecord) throws IOException {
        return getComponent().createClient(walletRecord);
    }
}