package io.nessus.aries.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;

import io.nessus.aries.coms.WebSocketEventHandler.WebSocketEvent;

public class V1CredentialHolderEventConsumer extends AbstractEventConsumer<V1CredentialExchange> {

    private V1CredentialExchange credentialExchange;
    private CountDownLatch offerReceivedLatch = new CountDownLatch(1);
    private CountDownLatch credentialReceivedLatch = new CountDownLatch(1);
    private CountDownLatch credentialAckedLatch = new CountDownLatch(1);

    public V1CredentialHolderEventConsumer() {
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
        if (CredentialExchangeRole.HOLDER == cex.getRole() && CredentialExchangeState.OFFER_RECEIVED == cex.getState()) {
            credentialExchange = cex;
            offerReceivedLatch.countDown();
        }
        else if (CredentialExchangeRole.HOLDER == cex.getRole() && CredentialExchangeState.CREDENTIAL_RECEIVED == cex.getState()) {
            credentialExchange = cex;
            credentialReceivedLatch.countDown();
        }
        else if (CredentialExchangeRole.HOLDER == cex.getRole() && CredentialExchangeState.CREDENTIAL_ACKED == cex.getState()) {
            credentialExchange = cex;
            credentialAckedLatch.countDown();
        }
    }
    
    public boolean awaitOfferReceived(long timeout, TimeUnit unit) throws InterruptedException {
        return offerReceivedLatch.await(timeout, unit);
    }
    
    public boolean awaitCredentialReceived(long timeout, TimeUnit unit) throws InterruptedException {
        return credentialReceivedLatch.await(timeout, unit);
    }
    
    public boolean awaitCredentialAcked(long timeout, TimeUnit unit) throws InterruptedException {
        return credentialAckedLatch.await(timeout, unit);
    }
}