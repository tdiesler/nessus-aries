package org.apache.camel.component.aries.handler;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.apache.camel.component.aries.UnsupportedServiceException;
import org.hyperledger.acy_py.generated.model.ConnectionInvitation;
import org.hyperledger.aries.api.connection.ConnectionReceiveInvitationFilter;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.CreateInvitationRequest;
import org.hyperledger.aries.api.connection.CreateInvitationResponse;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;

public class ConnectionsServiceHandler extends AbstractServiceHandler {
    
    public ConnectionsServiceHandler(HyperledgerAriesEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange, String service) throws Exception {
        if (service.equals("/connections/create-invitation")) {
            CreateInvitationRequest reqObj = getBodyOptional(exchange, CreateInvitationRequest.class);
            if (reqObj == null)
                reqObj = CreateInvitationRequest.builder().build();
            CreateInvitationResponse resObj = createClient().connectionsCreateInvitation(reqObj).get();
            exchange.getIn().setBody(resObj);
        }
        else if (service.equals("/connections/receive-invitation")) {
            ReceiveInvitationRequest reqObj = getBodyOptional(exchange, ReceiveInvitationRequest.class);
            if (reqObj == null) {
                ConnectionInvitation invitation = assertBody(exchange, ConnectionInvitation.class);
                reqObj = ReceiveInvitationRequest.builder()
                        .recipientKeys(invitation.getRecipientKeys())
                        .serviceEndpoint(invitation.getServiceEndpoint())
                        .build();
            }
            ConnectionReceiveInvitationFilter filter = getHeaderOptional(exchange, ConnectionReceiveInvitationFilter.class);
            ConnectionRecord resObj = createClient().connectionsReceiveInvitation(reqObj, filter).get();
            exchange.getIn().setBody(resObj);
        }
        else throw new UnsupportedServiceException(service);
    }
}