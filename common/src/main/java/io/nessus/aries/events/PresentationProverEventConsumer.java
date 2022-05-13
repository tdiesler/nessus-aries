package io.nessus.aries.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;

import io.nessus.aries.coms.WebSocketEventHandler.WebSocketEvent;

public class PresentationProverEventConsumer extends AbstractEventConsumer<PresentationExchangeRecord> {

    private PresentationExchangeRecord presentationExchangeRecord;
    private CountDownLatch requestReceivedLatch = new CountDownLatch(1);
    private CountDownLatch presentationAckedLatch = new CountDownLatch(1);

    public PresentationProverEventConsumer() {
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
        if (PresentationExchangeRole.PROVER == pex.getRole() && PresentationExchangeState.REQUEST_RECEIVED == pex.getState()) {
            presentationExchangeRecord = pex;
            requestReceivedLatch.countDown();
        }
        if (PresentationExchangeRole.PROVER == pex.getRole() && PresentationExchangeState.PRESENTATION_ACKED == pex.getState()) {
            presentationExchangeRecord = pex;
            presentationAckedLatch.countDown();
        }
    }
    
    public boolean awaitRequestReceived(long timeout, TimeUnit unit) throws InterruptedException {
        return requestReceivedLatch.await(timeout, unit);
    }
    
    public boolean awaitPresentationAcked(long timeout, TimeUnit unit) throws InterruptedException {
        return presentationAckedLatch.await(timeout, unit);
    }
}