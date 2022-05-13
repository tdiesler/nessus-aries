package io.nessus.aries.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;

import io.nessus.aries.coms.WebSocketEventHandler.WebSocketEvent;

public class V1CredentialIssuerEventConsumer extends AbstractEventConsumer<V1CredentialExchange> {

    private V1CredentialExchange credentialExchange;
    private CountDownLatch offerSentLatch = new CountDownLatch(1);
    private CountDownLatch requestReceivedLatch = new CountDownLatch(1);
    private CountDownLatch credentialRevokedLatch = new CountDownLatch(1);

    public V1CredentialIssuerEventConsumer() {
        super(V1CredentialExchange.class);
    }

    public V1CredentialExchange getCredentialExchange() {
        return credentialExchange;
    }

    @Override
    public void accept(WebSocketEvent ev) throws Exception {
        super.accept(ev);
        V1CredentialExchange cex = getPayload();
        log.info("{}: [@{}] {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), cex.getRole(), cex.getState(), cex); 
        if (CredentialExchangeRole.ISSUER == cex.getRole() && CredentialExchangeState.OFFER_SENT == cex.getState()) {
            credentialExchange = cex;
            offerSentLatch.countDown();
        }
        if (CredentialExchangeRole.ISSUER == cex.getRole() && CredentialExchangeState.REQUEST_RECEIVED == cex.getState()) {
            credentialExchange = cex;
            requestReceivedLatch.countDown();
        }
        if (CredentialExchangeRole.ISSUER == cex.getRole() && CredentialExchangeState.CREDENTIAL_REVOKED == cex.getState()) {
            credentialExchange = cex;
            credentialRevokedLatch.countDown();
        }
    }
    
    public boolean awaitOfferSent(long timeout, TimeUnit unit) throws InterruptedException {
        return offerSentLatch.await(timeout, unit);
    }
    
    public boolean awaitRequestReceived(long timeout, TimeUnit unit) throws InterruptedException {
        return requestReceivedLatch.await(timeout, unit);
    }
    
    public boolean awaitCredentialRevoked(long timeout, TimeUnit unit) throws InterruptedException {
        return credentialRevokedLatch.await(timeout, unit);
    }
}