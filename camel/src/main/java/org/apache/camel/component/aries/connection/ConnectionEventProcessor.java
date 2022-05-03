package org.apache.camel.component.aries.connection;

import static org.apache.camel.component.aries.utils.ProcessorSupport.getHyperledgerAriesComponent;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import io.nessus.aries.wallet.NessusWallet;

public class ConnectionEventProcessor implements Processor {

    private final String walletName;
    
    public ConnectionEventProcessor(String walletName) {
        this.walletName = walletName;
    }
    
    @Override
    public void process(Exchange exchange) throws Exception {
        NessusWallet walletRecord = getHyperledgerAriesComponent(exchange).assertWallet(walletName);
        ConnectionEventConsumer eventConsumer = new ConnectionEventConsumer();
        exchange.getIn().setHeader(ConnectionEventConsumer.getHeaderKey(walletName), eventConsumer);
        eventConsumer.subscribeTo(walletRecord.getWebSocketEventHandler());
    }
}