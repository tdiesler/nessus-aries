package org.apache.camel.component.aries.handler;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.apache.camel.component.aries.UnsupportedServiceException;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationRequest;
import org.hyperledger.aries.api.present_proof.PresentationRequestCredentials;
import org.hyperledger.aries.api.present_proof.PresentationRequestCredentialsFilter;

public class PresentProofServiceHandler extends AbstractServiceHandler {
    
    public PresentProofServiceHandler(HyperledgerAriesEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange, String service) throws Exception {
        if (service.equals("/present-proof/send-request")) {
            PresentProofRequest reqObj = assertBody(exchange, PresentProofRequest.class);
            PresentationExchangeRecord resObj = createClient().presentProofSendRequest(reqObj).get();
            exchange.getIn().setBody(resObj);
        }
        else if (service.startsWith("/present-proof/records/")) {
            String presentationExchangeId = getServicePathToken(service, 2);
            if (service.endsWith("/credentials")) {
                PresentationRequestCredentialsFilter reqObj = assertBody(exchange, PresentationRequestCredentialsFilter.class);
                List<PresentationRequestCredentials> resObj = createClient().presentProofRecordsCredentials(presentationExchangeId, reqObj).get();
                exchange.getIn().setBody(resObj);
            }
            else if (service.endsWith("/send-presentation")) {
                PresentationRequest reqObj = assertBody(exchange, PresentationRequest.class);
                PresentationExchangeRecord resObj = createClient().presentProofRecordsSendPresentation(presentationExchangeId, reqObj).get();
                exchange.getIn().setBody(resObj);
            }
            else if (service.endsWith("/verify-presentation")) {
                PresentationExchangeRecord resObj = createClient().presentProofRecordsVerifyPresentation(presentationExchangeId).get();
                exchange.getIn().setBody(resObj);
            }
            else throw new UnsupportedServiceException(service);
        }
        else throw new UnsupportedServiceException(service);
    }
}