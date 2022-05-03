package org.apache.camel.component.aries.connection;

import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.hyperledger.aries.api.connection.ConnectionRecord;

import io.nessus.aries.util.AssertState;

public class ConnectionResultProcessor implements Processor {

    private final String inviterName;
    private final String inviteeName;
    
    public ConnectionResultProcessor(String inviterName, String inviteeName) {
        this.inviterName = inviterName;
        this.inviteeName = inviteeName;
    }
    
    @Override
    public void process(Exchange exchange) throws Exception {
        ConnectionEventConsumer inviterConsumer = ConnectionEventConsumer.getEventConsumer(exchange, inviterName);
        ConnectionEventConsumer inviteeConsumer = ConnectionEventConsumer.getEventConsumer(exchange, inviteeName);
        AssertState.isTrue(inviterConsumer.await(10, TimeUnit.SECONDS), "No ACTIVE Connection event for: " + inviterName);
        AssertState.isTrue(inviteeConsumer.await(10, TimeUnit.SECONDS), "No ACTIVE Connection event for: " + inviteeName);
        exchange.getIn().setBody(new ConnectionResult(inviterConsumer.getPayload(), inviteeConsumer.getPayload()));
    }

    public static class ConnectionResult {
        public final ConnectionRecord inviterConnection;
        public final ConnectionRecord inviteeConnection;
        public ConnectionResult(ConnectionRecord inviterConnection, ConnectionRecord inviteeConnection) {
            this.inviterConnection = inviterConnection;
            this.inviteeConnection = inviteeConnection;
        }
    }
}