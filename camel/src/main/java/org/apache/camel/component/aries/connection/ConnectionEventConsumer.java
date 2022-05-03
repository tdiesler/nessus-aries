package org.apache.camel.component.aries.connection;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.utils.AbstractEventConsumer;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;

import io.nessus.aries.coms.WebSocketEventHandler.WebSocketEvent;

public class ConnectionEventConsumer extends AbstractEventConsumer<ConnectionRecord> {

    private CountDownLatch peerConnectionLatch = new CountDownLatch(1);
    
    public ConnectionEventConsumer() {
        super(ConnectionRecord.class);
    }

    public static String getHeaderKey(String walletName) {
        return walletName + ConnectionEventConsumer.class.getSimpleName();
    }
    
    public static ConnectionEventConsumer getEventConsumer(Exchange exchange, String walletName) {
        return exchange.getIn().getHeader(getHeaderKey(walletName), ConnectionEventConsumer.class);
    }
    
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return peerConnectionLatch.await(timeout, unit);
    } 
    
    @Override
    public void accept(WebSocketEvent ev) throws Exception {
        super.accept(ev);
        ConnectionRecord con = getPayload();
        String thisName = ev.getThisWalletName();
        String theirName = ev.getTheirWalletName();
        log.info("{}: [@{}] {} {} {}", thisName, theirName, con.getTheirRole(), con.getState(), con);
        if (ConnectionState.ACTIVE == con.getState()) {
            getSubscriber().cancelSubscription();
            peerConnectionLatch.countDown();
        }
    }
}