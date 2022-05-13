package io.nessus.aries.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;

import io.nessus.aries.coms.WebSocketEventHandler.WebSocketEvent;

public class PresentationVerifierEventConsumer extends AbstractEventConsumer<PresentationExchangeRecord> {

    private PresentationExchangeRecord presentationExchangeRecord;
    private CountDownLatch presentationReceivedLatch = new CountDownLatch(1);
    private CountDownLatch presentationVerifiedLatch = new CountDownLatch(1);

    public PresentationVerifierEventConsumer() {
        super(PresentationExchangeRecord.class);
    }

    public PresentationExchangeRecord getPresentationExchangeRecord() {
        return presentationExchangeRecord;
    }

    @Override
    public void accept(WebSocketEvent ev) throws Exception {
        super.accept(ev);
        PresentationExchangeRecord pex = getPayload();
        log.info("{}: [@{}] {} {} {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), pex.getRole(), pex.getState(), pex); 
        if (PresentationExchangeRole.VERIFIER == pex.getRole() && PresentationExchangeState.PRESENTATION_RECEIVED == pex.getState()) {
            presentationExchangeRecord = pex;
            presentationReceivedLatch.countDown();
        }
        if (PresentationExchangeRole.VERIFIER == pex.getRole() && PresentationExchangeState.VERIFIED == pex.getState()) {
            presentationExchangeRecord = pex;
            presentationVerifiedLatch.countDown();
        }
    }
    
    public boolean awaitPresentationReceived(long timeout, TimeUnit unit) throws InterruptedException {
        return presentationReceivedLatch.await(timeout, unit);
    }
    
    public boolean awaitPresentationVerified(long timeout, TimeUnit unit) throws InterruptedException {
        return presentationVerifiedLatch.await(timeout, unit);
    }
}