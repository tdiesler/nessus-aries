package org.apache.camel.component.aries.processor;

import static org.apache.camel.component.aries.processor.ProcessorSupport.getHyperledgerAriesComponent;
import static org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole.HOLDER;
import static org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole.ISSUER;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;

import io.nessus.aries.coms.WebSocketEventHandler.WebSocketEvent;
import io.nessus.aries.util.AssertState;
import io.nessus.aries.wallet.NessusWallet;

public class CredentialExchangeEventProcessor implements Processor {

    private final String walletName;
    private final CredentialExchangeRole role;
    
    public CredentialExchangeEventProcessor(String walletName, CredentialExchangeRole role) {
        this.walletName = walletName;
        this.role = role;
    }
    
    @Override
    public void process(Exchange exchange) throws Exception {
        NessusWallet walletRecord = getHyperledgerAriesComponent(exchange).assertWallet(walletName);
        CredentialExchangeEventConsumer eventConsumer = new CredentialExchangeEventConsumer();
        exchange.getIn().setHeader(getHeaderKey(role), eventConsumer);
        eventConsumer.subscribeTo(walletRecord.getWebSocketEventHandler());
    }

    public static String getHeaderKey(CredentialExchangeRole role) {
        return (ISSUER == role ? "Issuer" : "Holder") + CredentialExchangeEventConsumer.class.getSimpleName();
    }
    
    public static CredentialExchangeEventConsumer getEventConsumer(Exchange exchange, CredentialExchangeRole role) {
        return exchange.getIn().getHeader(getHeaderKey(role), CredentialExchangeEventConsumer.class);
    }
    
    public static V1CredentialExchange getIssuerCredentialExchange(Exchange exchange) {
        CredentialExchangeEventConsumer eventConsumer = getEventConsumer(exchange, ISSUER);
        return eventConsumer.getIssuerCredentialExchange();
    }

    public static V1CredentialExchange getHolderCredentialExchange(Exchange exchange) {
        CredentialExchangeEventConsumer eventConsumer = getEventConsumer(exchange, HOLDER);
        return eventConsumer.getHolderCredentialExchange();
    }

    public static void awaitHolderOfferReceived(Exchange exchange, long timeout, TimeUnit unit) throws InterruptedException {
        CredentialExchangeEventConsumer eventConsumer = getEventConsumer(exchange, HOLDER);
        boolean result = eventConsumer.awaitHolderOfferReceived(timeout, unit);
        AssertState.isTrue(result, "No HOLDER OFFER_RECEIVED");
        exchange.getIn().setHeader("HolderCredentialExchange", eventConsumer.getHolderCredentialExchange());
    } 
    
    public static void awaitIssuerRequestReceived(Exchange exchange, long timeout, TimeUnit unit) throws InterruptedException {
        CredentialExchangeEventConsumer eventConsumer = getEventConsumer(exchange, ISSUER);
        boolean result = eventConsumer.awaitIssuerRequestReceived(timeout, unit);
        AssertState.isTrue(result, "No ISSUER REQUEST_RECEIVED");
        exchange.getIn().setHeader("IssuerCredentialExchange", eventConsumer.getIssuerCredentialExchange());
    } 
    
    public static void awaitHolderCredentialReceived(Exchange exchange, long timeout, TimeUnit unit) throws InterruptedException {
        CredentialExchangeEventConsumer eventConsumer = getEventConsumer(exchange, HOLDER);
        boolean result = eventConsumer.awaitHolderCredentialReceived(timeout, unit);
        AssertState.isTrue(result, "No HOLDER CREDENTIAL_RECEIVED");
        exchange.getIn().setHeader("HolderCredentialExchange", eventConsumer.getHolderCredentialExchange());
    } 
    
    public static void awaitHolderCredentialAcked(Exchange exchange, long timeout, TimeUnit unit) throws InterruptedException {
        CredentialExchangeEventConsumer eventConsumer = getEventConsumer(exchange, HOLDER);
        boolean result = eventConsumer.awaitHolderCredentialAcked(timeout, unit);
        AssertState.isTrue(result, "No HOLDER CREDENTIAL_ACKED");
        exchange.getIn().setHeader("HolderCredentialExchange", eventConsumer.getHolderCredentialExchange());
    } 
    
    class CredentialExchangeEventConsumer extends AbstractEventConsumer<V1CredentialExchange> {

        V1CredentialExchange[] issuerCredentialExchange = new V1CredentialExchange[1];
        V1CredentialExchange[] holderCredentialExchange = new V1CredentialExchange[1];
        CountDownLatch holderOfferReceived = new CountDownLatch(1);
        CountDownLatch issuerRequestReceived = new CountDownLatch(1);
        CountDownLatch holderCredentialReceived = new CountDownLatch(1);
        CountDownLatch holderCredentialAcked = new CountDownLatch(1);
        
        public CredentialExchangeEventConsumer() {
            super(V1CredentialExchange.class);
        }

        public V1CredentialExchange getIssuerCredentialExchange() {
            return issuerCredentialExchange[0];
        }

        public V1CredentialExchange getHolderCredentialExchange() {
            return holderCredentialExchange[0];
        }

        public boolean awaitHolderOfferReceived(long timeout, TimeUnit unit) throws InterruptedException {
            return holderOfferReceived.await(timeout, unit);
        } 
        
        public boolean awaitIssuerRequestReceived(long timeout, TimeUnit unit) throws InterruptedException {
            return issuerRequestReceived.await(timeout, unit);
        } 
        
        public boolean awaitHolderCredentialReceived(long timeout, TimeUnit unit) throws InterruptedException {
            return holderCredentialReceived.await(timeout, unit);
        } 
        
        public boolean awaitHolderCredentialAcked(long timeout, TimeUnit unit) throws InterruptedException {
            return holderCredentialAcked.await(timeout, unit);
        } 
        
        @Override
        public void accept(WebSocketEvent ev) throws Exception {
            super.accept(ev);
            V1CredentialExchange cex = getPayload();
            log.info("{}: [@{}] {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), cex.getRole(), cex.getState(), cex);
            if (CredentialExchangeRole.ISSUER == role) {
                if (CredentialExchangeRole.ISSUER == cex.getRole() && CredentialExchangeState.REQUEST_RECEIVED == cex.getState()) {
                    getSubscriber().cancelSubscription();
                    issuerCredentialExchange[0] = cex;
                    issuerRequestReceived.countDown();
                }
            }
            if (CredentialExchangeRole.HOLDER == role) {
                if (CredentialExchangeRole.HOLDER == cex.getRole() && CredentialExchangeState.OFFER_RECEIVED == cex.getState()) {
                    holderCredentialExchange[0] = cex;
                    holderOfferReceived.countDown();
                }
                else if (CredentialExchangeRole.HOLDER == cex.getRole() && CredentialExchangeState.CREDENTIAL_RECEIVED == cex.getState()) {
                    holderCredentialExchange[0] = cex;
                    holderCredentialReceived.countDown();
                }
                else if (CredentialExchangeRole.HOLDER == cex.getRole() && CredentialExchangeState.CREDENTIAL_ACKED == cex.getState()) {
                    getSubscriber().cancelSubscription();
                    holderCredentialExchange[0] = cex;
                    holderCredentialAcked.countDown();
                }
            }
        }
    }
}