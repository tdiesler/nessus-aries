package org.apache.camel.component.aries.handler;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.apache.camel.component.aries.UnsupportedServiceException;
import org.hyperledger.aries.api.issue_credential_v1.IssueCredentialRecordsFilter;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialIssueRequest;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialOfferRequest;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialStoreRequest;

public class IssueCredentialServiceHandler extends AbstractServiceHandler {
    
    public IssueCredentialServiceHandler(HyperledgerAriesEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange, String service) throws Exception {
        if (service.equals("/issue-credential/send-offer")) {
            V1CredentialOfferRequest reqObj = assertBody(exchange, V1CredentialOfferRequest.class);
            V1CredentialExchange resObj = createClient().issueCredentialSendOffer(reqObj).get();
            exchange.getIn().setBody(resObj);
        }
        else if (service.equals("/issue-credential/records")) {
            IssueCredentialRecordsFilter reqObj = assertBody(exchange, IssueCredentialRecordsFilter.class);
            List<V1CredentialExchange> resObj = createClient().issueCredentialRecords(reqObj).get();
            exchange.getIn().setBody(resObj);
        }
        else if (service.startsWith("/issue-credential/records")) {
            String credentialExchangeId = getServicePathToken(service, 2);
            if (service.endsWith("/send-request")) {
                V1CredentialExchange resObj = createClient().issueCredentialRecordsSendRequest(credentialExchangeId).get();
                exchange.getIn().setBody(resObj);
            }
            else if (service.endsWith("/issue")) {
                V1CredentialIssueRequest reqObj = assertBody(exchange, V1CredentialIssueRequest.class);
                V1CredentialExchange resObj = createClient().issueCredentialRecordsIssue(credentialExchangeId, reqObj).get();
                exchange.getIn().setBody(resObj);
            }
            else if (service.endsWith("/store")) {
                V1CredentialStoreRequest reqObj = assertBody(exchange, V1CredentialStoreRequest.class);
                V1CredentialExchange resObj = createClient().issueCredentialRecordsStore(credentialExchangeId, reqObj).get();
                exchange.getIn().setBody(resObj);
            }
            else throw new UnsupportedServiceException(service);
        }
        else throw new UnsupportedServiceException(service);
    }
}