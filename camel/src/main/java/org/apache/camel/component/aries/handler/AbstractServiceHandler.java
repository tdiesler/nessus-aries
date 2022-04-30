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

import io.nessus.aries.AgentConfiguration;
import io.nessus.aries.AriesClientFactory;
import io.nessus.aries.util.AssertArg;
import io.nessus.aries.util.AssertState;

public abstract class AbstractServiceHandler implements ServiceHandler {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final HyperledgerAriesEndpoint endpoint;
    protected final String service;
    
    public AbstractServiceHandler(HyperledgerAriesEndpoint endpoint, String service) {
        this.endpoint = endpoint;
        this.service = service;
    }
    
    public <T> T assertBody(Exchange exchange, Class<T> type) {
        T body = exchange.getIn().getBody(type);
        AssertState.notNull(body, "Cannot obtain body of type: " + type.getName());
        return body;
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
        AgentConfiguration agentConfig = getComponent().getAgentConfiguration();
        return AriesClientFactory.baseClient(agentConfig);
    }
    
    public AriesClient createClient() throws IOException {
        String walletName = getConfiguration().getWallet();
        WalletRecord walletRecord = getComponent().getWalletByName(walletName);
        AssertState.notNull(walletRecord, "No WalletRecord for: " + walletName);
        return createClient(walletRecord);
    }
    
    public AriesClient createClient(WalletRecord walletRecord) throws IOException {
        AssertArg.notNull(walletRecord, "No WalletRecord");
        AgentConfiguration agentConfig = getComponent().getAgentConfiguration();
        return AriesClientFactory.createClient(walletRecord, agentConfig);
    }

}