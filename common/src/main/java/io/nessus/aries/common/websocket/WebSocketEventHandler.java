package io.nessus.aries.common.websocket;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.SubmissionPublisher;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.discover_features.DiscoverFeatureEvent;
import org.hyperledger.aries.api.endorser.EndorseTransactionRecord;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueIndyCredentialEvent;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueLDCredentialEvent;
import org.hyperledger.aries.api.message.BasicMessage;
import org.hyperledger.aries.api.message.ProblemReport;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof_v2.V20PresExRecord;
import org.hyperledger.aries.api.revocation.RevocationEvent;
import org.hyperledger.aries.api.revocation.RevocationNotificationEvent;
import org.hyperledger.aries.api.settings.Settings;
import org.hyperledger.aries.api.trustping.PingEvent;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.aries.webhook.EventParser;
import org.hyperledger.aries.webhook.EventType;
import org.hyperledger.aries.webhook.IEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.nessus.aries.common.Configuration;
import io.nessus.aries.common.SafeConsumer;
import io.nessus.aries.common.WalletRegistry;

public class WebSocketEventHandler implements IEventHandler, Closeable {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    private final SubmissionPublisher<WebSocketEvent> webSocketEventPublisher = new SubmissionPublisher<>(Executors.newSingleThreadExecutor(), 10);
    private final EventParser parser = new EventParser();

    private final WalletRegistry walletRegistry;
    private WalletRecord thisWallet;
    
    private WebSocketEventHandler(WalletRegistry walletRegistry, List<EventSubscriber<WebSocketEvent>> subscribers) {
        this.walletRegistry = walletRegistry;
        for (EventSubscriber<WebSocketEvent> sub : subscribers)
            this.webSocketEventPublisher.subscribe(sub);
    }

    // Set internally
    void setThisWallet(WalletRecord thisWallet) {
        this.thisWallet = thisWallet;
    }

    public Optional<WalletRecord> getWallet(String walletId) {
        Optional<WalletRecord> result = Optional.ofNullable(null);
        if (walletRegistry != null) 
            result = walletRegistry.getWallet(walletId);
        return result;
    }
    
    public String getWalletName(String walletId) {
        Optional<WalletRecord> optional = getWallet(walletId);
        return optional.isPresent() ? optional.get().getSettings().getWalletName() : walletId;
    }
    
    public String getThisWalletId() {
        return thisWallet.getWalletId();
    }

    public String getThisWalletName() {
        return thisWallet.getSettings().getWalletName();
    }

    @Override
    public void close() {
        webSocketEventPublisher.close();
    }
    
    public <T> EventSubscriber<WebSocketEvent> subscribe(String walletId, Class<T> eventType, SafeConsumer<WebSocketEvent> consumer) {
        Objects.requireNonNull(consumer);
        EventSubscriber<WebSocketEvent> subscriber = new FilteringEventSubscriber(walletId, eventType, consumer);
        webSocketEventPublisher.subscribe(subscriber);
        return subscriber;
    }
    
    public EventSubscriber<WebSocketEvent> subscribe(List<String> walletIds, List<Class<?>> eventTypes, SafeConsumer<WebSocketEvent> consumer) {
        Objects.requireNonNull(consumer);
        EventSubscriber<WebSocketEvent> subscriber = new FilteringEventSubscriber(walletIds, eventTypes, consumer);
        webSocketEventPublisher.subscribe(subscriber);
        return subscriber;
    }
    
    @Override
    public void handleEvent(String topic, String payload) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleEvent(String theirWalletId, String topic, String payload) {
        try {
            Object value;
            if (EventType.CONNECTIONS.topicEquals(topic)) {
                value = parser.parseValueSave(payload, ConnectionRecord.class).orElseThrow();
            } else if (EventType.PRESENT_PROOF.topicEquals(topic)) {
                value = parser.parseValueSave(payload, PresentationExchangeRecord.class).orElseThrow();
            } else if (EventType.PRESENT_PROOF_V2.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V20PresExRecord.class).orElseThrow();
            } else if (EventType.ISSUE_CREDENTIAL.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V1CredentialExchange.class).orElseThrow();
            } else if (EventType.ISSUE_CREDENTIAL_V2.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V20CredExRecord.class).orElseThrow();
            } else if (EventType.ISSUE_CREDENTIAL_V2_INDY.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V2IssueIndyCredentialEvent.class).orElseThrow();
            } else if (EventType.ISSUE_CREDENTIAL_V2_LD_PROOF.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V2IssueLDCredentialEvent.class).orElseThrow();
            } else if (EventType.BASIC_MESSAGES.topicEquals(topic)) {
                value = parser.parseValueSave(payload, BasicMessage.class).orElseThrow();
            } else if (EventType.PING.topicEquals(topic)) {
                value = parser.parseValueSave(payload, PingEvent.class).orElseThrow();
            } else if (EventType.ISSUER_CRED_REV.topicEquals(topic)) {
                value = parser.parseValueSave(payload, RevocationEvent.class).orElseThrow();
            } else if (EventType.ENDORSE_TRANSACTION.topicEquals(topic)) {
                value = parser.parseValueSave(payload, EndorseTransactionRecord.class).orElseThrow();
            } else if (EventType.PROBLEM_REPORT.topicEquals(topic)) {
                value = parser.parseValueSave(payload, ProblemReport.class).orElseThrow();
            } else if (EventType.DISCOVER_FEATURE.topicEquals(topic)) {
                value = parser.parseValueSave(payload, DiscoverFeatureEvent.class).orElseThrow();
            } else if (EventType.REVOCATION_NOTIFICATION.topicEquals(topic)) {
                value = parser.parseValueSave(payload, RevocationNotificationEvent.class).orElseThrow();
            } else if (EventType.SETTINGS.topicEquals(topic)) {
                value = parser.parseValueSave(payload, Settings.class).orElseThrow();
            } else {
                log.warn("Unsupported event topic: {}", topic);
                if (log.isDebugEnabled()) {
                    JsonElement json = JsonParser.parseString(payload);
                    log.debug(GsonConfig.prettyPrinter().toJson(json));
                }
                return;
            }
            if (log.isTraceEnabled())
                log.trace("{}: [@{}] {}", getThisWalletName(), getWalletName(theirWalletId), value);
            if (webSocketEventPublisher.hasSubscribers())
                webSocketEventPublisher.submit(new WebSocketEvent(theirWalletId, topic, value));
        } catch (Throwable e) {
            log.error("Error in webhook event handler:", e);
        }
    }
    
    public class WebSocketEvent {
        private final String theirWalletId;
        private final String topic;
        private final Object payload;
        
        WebSocketEvent(String theirWalletId, String topic, Object payload) {
            this.theirWalletId = theirWalletId;
            this.topic = topic;
            this.payload = payload;
        }

        public WebSocketEventHandler getEventHandler() {
            return WebSocketEventHandler.this;
        }
        
        public AriesClient createClient() throws IOException {
            return Configuration.createClient(getThisWallet());
        }
        
        public String getTopic() {
            return topic;
        }

        public Class<?> getEventType() {
            return payload.getClass();
        }

        public WalletRecord getThisWallet() {
            return WebSocketEventHandler.this.thisWallet;
        }

        public String getThisWalletId() {
            return WebSocketEventHandler.this.getThisWalletId();
        }

        public String getThisWalletName() {
            return WebSocketEventHandler.this.getThisWalletName();
        }

        public String getTheirWalletId() {
            return theirWalletId;
        }

        public String getTheirWalletName() {
            return WebSocketEventHandler.this.getWalletName(theirWalletId);
        }

        @SuppressWarnings("unchecked")
        public <T> T getPayload(Class<T> type) {
            return (T) payload;
        }
        
        public Object getPayload() {
            return payload;
        }
    }
    
    public static class Builder {
        
        private WalletRegistry walletRegistry;
        private List<EventSubscriber<WebSocketEvent>> subscribers = new ArrayList<>();

        public Builder walletRegistry(WalletRegistry walletRegistry) {
            this.walletRegistry = walletRegistry;
            return this;
        }

        public <T> Builder subscribe(String walletId, Class<T> eventType, SafeConsumer<WebSocketEvent> consumer) {
            Objects.requireNonNull(consumer);
            EventSubscriber<WebSocketEvent> subscriber = new FilteringEventSubscriber(walletId, eventType, consumer);
            this.subscribers.add(subscriber);
            return this;
        }
        
        public Builder subscribe(List<String> walletIds, List<Class<?>> eventTypes, SafeConsumer<WebSocketEvent> consumer) {
            Objects.requireNonNull(consumer);
            EventSubscriber<WebSocketEvent> subscriber = new FilteringEventSubscriber(walletIds, eventTypes, consumer);
            this.subscribers.add(subscriber);
            return this;
        }
        
        public WebSocketEventHandler build() {
            return new WebSocketEventHandler(walletRegistry, subscribers);
        }
    }
}