package io.nessus.aries.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;

import io.nessus.aries.coms.WebSocketEventHandler.WebSocketEvent;

/**
 * RFC 0160: Connection Protocol with multitenant wallets
 * 
 * Requires --auto-ping-connection otherwise Inviter gets stuck in state RESPONSE
 * 
 * https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol
 */
public class ConnectionEventConsumer extends AbstractEventConsumer<ConnectionRecord> {

    private ConnectionRecord connectionRecord;
    private CountDownLatch peerConnectionLatch = new CountDownLatch(1);

    public ConnectionEventConsumer() {
        super(ConnectionRecord.class);
    }

    public ConnectionRecord getConnectionRecord() {
        return connectionRecord;
    }

    @Override
    public void accept(WebSocketEvent ev) throws Exception {
        super.accept(ev);
        String thisName = ev.getThisWalletName();
        String theirName = ev.getTheirWalletName();
        ConnectionRecord con = connectionRecord = getPayload();
        log.info("{}: [@{}] {} {} {}", thisName, theirName, con.getTheirRole(), con.getState(), con);
        if (ConnectionState.ACTIVE == con.getState()) {
            cancelSubscription();
            peerConnectionLatch.countDown();
        }
    }
    
    public boolean awaitConnectionActive(long timeout, TimeUnit unit) throws InterruptedException {
        return peerConnectionLatch.await(timeout, unit);
    }
}