package io.nessus.aries.events;

import static org.hyperledger.aries.api.revocation.RevocationEvent.RevocationEventState.REVOKED;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hyperledger.aries.api.revocation.RevocationEvent;

import io.nessus.aries.coms.WebSocketEventHandler.WebSocketEvent;

public class RevocationEventConsumer extends AbstractEventConsumer<RevocationEvent> {

    private RevocationEvent revocationEvent;
    private CountDownLatch credentialRevokedLatch = new CountDownLatch(1);

    public RevocationEventConsumer() {
        super(RevocationEvent.class);
    }

    public RevocationEvent getRevocationEvent() {
        return revocationEvent;
    }

    @Override
    public void accept(WebSocketEvent ev) throws Exception {
        super.accept(ev);
        RevocationEvent revoc = revocationEvent = ev.getPayload(RevocationEvent.class);
        log.info("{}: [@{}] {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), revoc.getState(), revoc); 
        if (REVOKED.equals(revoc.getState())) {
            credentialRevokedLatch.countDown();
        }
    }
    
    public boolean awaitCredentialRevoked(long timeout, TimeUnit unit) throws InterruptedException {
        return credentialRevokedLatch.await(timeout, unit);
    }
}