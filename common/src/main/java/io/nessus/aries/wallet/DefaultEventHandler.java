/*-
 * #%L
 * Nessus Aries :: Common
 * %%
 * Copyright (C) 2022 Nessus
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.nessus.aries.wallet;

import java.io.IOException;
import java.util.Objects;

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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.nessus.aries.AriesClientFactory;
import io.nessus.aries.util.SafeConsumer;

public class DefaultEventHandler implements IEventHandler {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final Gson pretty = GsonConfig.prettyPrinter();

    private final EventParser parser = new EventParser();

    private final WalletRegistry walletRegistry;
    private WalletRecord thisWallet;
    
    public DefaultEventHandler(WalletRecord thisWallet, WalletRegistry walletRegistry) {
        Objects.nonNull(thisWallet);
        this.walletRegistry = walletRegistry;
        this.thisWallet = thisWallet;
    }

    public WalletRecord getWallet(String walletId) {
        WalletRecord result = null;
        if (walletRegistry != null) 
            result = walletRegistry.getWallet(walletId);
        return result;
    }
    
    public String getWalletName(String walletId) {
        WalletRecord wallet = getWallet(walletId);
        return wallet != null ? wallet.getSettings().getWalletName() : walletId;
    }
    
    public String getThisWalletId() {
        return thisWallet.getWalletId();
    }

    public String getThisWalletName() {
        return thisWallet.getSettings().getWalletName();
    }

    @Override
    public void handleEvent(String topic, String payload) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleEvent(String theirWalletId, String topic, String payload) {
        try {
            Object value;
            SafeConsumer<WebSocketEvent> consumer = ev -> handleGenericEvent(ev);
            if (EventType.CONNECTIONS.topicEquals(topic)) {
                value = parser.parseValueSave(payload, ConnectionRecord.class).orElseThrow();
                consumer = ev -> handleConnection(ev);
            } else if (EventType.PRESENT_PROOF.topicEquals(topic)) {
                value = parser.parseValueSave(payload, PresentationExchangeRecord.class).orElseThrow();
                consumer = ev -> handleProof(ev);
            } else if (EventType.PRESENT_PROOF_V2.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V20PresExRecord.class).orElseThrow();
                // [TODO]
            } else if (EventType.ISSUE_CREDENTIAL.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V1CredentialExchange.class).orElseThrow();
                consumer = ev -> handleCredential(ev);
            } else if (EventType.ISSUE_CREDENTIAL_V2.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V20CredExRecord.class).orElseThrow();
                // [TODO]
            } else if (EventType.ISSUE_CREDENTIAL_V2_INDY.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V2IssueIndyCredentialEvent.class).orElseThrow();
                // [TODO]
            } else if (EventType.ISSUE_CREDENTIAL_V2_LD_PROOF.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V2IssueLDCredentialEvent.class).orElseThrow();
                // [TODO]
            } else if (EventType.BASIC_MESSAGES.topicEquals(topic)) {
                value = parser.parseValueSave(payload, BasicMessage.class).orElseThrow();
                // [TODO]
            } else if (EventType.PING.topicEquals(topic)) {
                value = parser.parseValueSave(payload, PingEvent.class).orElseThrow();
                // [TODO]
            } else if (EventType.ISSUER_CRED_REV.topicEquals(topic)) {
                value = parser.parseValueSave(payload, RevocationEvent.class).orElseThrow();
                consumer = ev -> handleRevocation(ev);
            } else if (EventType.ENDORSE_TRANSACTION.topicEquals(topic)) {
                value = parser.parseValueSave(payload, EndorseTransactionRecord.class).orElseThrow();
                // [TODO]
            } else if (EventType.PROBLEM_REPORT.topicEquals(topic)) {
                value = parser.parseValueSave(payload, ProblemReport.class).orElseThrow();
                // [TODO]
            } else if (EventType.DISCOVER_FEATURE.topicEquals(topic)) {
                value = parser.parseValueSave(payload, DiscoverFeatureEvent.class).orElseThrow();
                // [TODO]
            } else if (EventType.REVOCATION_NOTIFICATION.topicEquals(topic)) {
                value = parser.parseValueSave(payload, RevocationNotificationEvent.class).orElseThrow();
                // [TODO]
            } else if (EventType.SETTINGS.topicEquals(topic)) {
                value = parser.parseValueSave(payload, Settings.class).orElseThrow();
                consumer = ev -> handleSettings(ev);
            } else {
                log.warn("Unsupported event topic: {}", topic);
                if (log.isDebugEnabled()) {
                    JsonElement json = JsonParser.parseString(payload);
                    log.debug(GsonConfig.prettyPrinter().toJson(json));
                }
                return;
            }
            
            consumer.accept(new WebSocketEvent(theirWalletId, topic, value));
            if (log.isTraceEnabled()) {
                log.trace("RequestBody\n{}", pretty.toJson(JsonParser.parseString(payload)));
            }
            
        } catch (Throwable e) {
            log.error("Error in webhook event handler:", e);
        }
    }
    
    public void handleSettings(WebSocketEvent ev) throws Exception {
        log.info("{}: {}", ev.getThisWalletName(), ev.getPayload());
    }

    public void handleConnection(WebSocketEvent ev) throws Exception {
        ConnectionRecord con = ev.getPayload(ConnectionRecord.class);
        log.info("{}: [@{}] {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), con.getTheirRole(), con.getState(), con);
    }

    public void handleCredential(WebSocketEvent ev) throws Exception {
        V1CredentialExchange cex = ev.getPayload(V1CredentialExchange.class);
        log.info("{}: [@{}] {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), cex.getRole(), cex.getState(), cex);
    }
    
    public void handleGenericEvent(WebSocketEvent ev) throws Exception {
        log.info("{}: [@{}] {}", ev.getThisWalletName(), ev.getTheirWalletName(), ev.getPayload());
    }

    public void handleRevocation(WebSocketEvent ev) throws Exception {
        RevocationEvent revoc = ev.getPayload(RevocationEvent.class);
        log.info("{}: [@{}] {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), revoc.getState(), revoc); 
    }

    public void handleProof(WebSocketEvent ev) throws Exception {
        PresentationExchangeRecord pex = ev.getPayload(PresentationExchangeRecord.class);
        log.info("{}: [@{}] {} {} {} {} {}", ev.getThisWalletName(), ev.getTheirWalletName(), pex.getRole(), pex.getState(), pex); 
    }

//    public void handleProofV2(String walletId, V20PresExRecord proof) throws Exception {
//        log.debug(LOG_MSG_MULTI, walletId, EventType.PRESENT_PROOF_V2, proof);
//    }
//
//
//    public void handleCredentialV2(String walletId, V20CredExRecord v20Credential) throws Exception {
//        log.debug(LOG_MSG_MULTI, walletId, EventType.ISSUE_CREDENTIAL_V2, v20Credential);
//    }
//
//    public void handleDiscoverFeature(String walletId, DiscoverFeatureEvent discoverFeature) throws Exception {
//        log.debug(LOG_MSG_MULTI, walletId, EventType.DISCOVER_FEATURE, discoverFeature);
//    }
//
//    public void handleIssueCredentialV2Indy(String walletId, V2IssueIndyCredentialEvent credentialInfo) throws Exception {
//        log.debug(LOG_MSG_MULTI, walletId, EventType.ISSUE_CREDENTIAL_V2_INDY, credentialInfo);
//    }
//
//    public void handleIssueCredentialV2LD(String walletId, V2IssueLDCredentialEvent credentialInfo) throws Exception {
//        log.debug(LOG_MSG_MULTI, walletId, EventType.ISSUE_CREDENTIAL_V2_LD_PROOF, credentialInfo);
//    }
//
//    public void handleBasicMessage(String walletId, BasicMessage message) throws Exception {
//        log.debug(LOG_MSG_MULTI, walletId, EventType.BASIC_MESSAGES, message);
//    }
//
//    public void handlePing(String walletId, PingEvent ping) throws Exception {
//        log.debug(LOG_MSG_MULTI, walletId, EventType.PING, ping);
//    }
//
//    public void handleRevocationNotification(String walletId, RevocationNotificationEvent revocationNotification) throws Exception {
//        log.debug(LOG_MSG_MULTI, walletId, EventType.REVOCATION_NOTIFICATION, revocationNotification);
//    }
//
//    public void handleRevocationRegistry(String walletId, IssuerRevRegRecord revocationRegistry) throws Exception {
//        log.debug(LOG_MSG_MULTI, walletId, EventType.REVOCATION_REGISTRY, revocationRegistry);
//    }
//
//    public void handleEndorseTransaction(String walletId, EndorseTransactionRecord transaction) throws Exception {
//        log.debug(LOG_MSG_MULTI, walletId, EventType.ENDORSE_TRANSACTION, transaction);
//    }
//
//    public void handleProblemReport(String walletId, ProblemReport report) throws Exception {
//        log.debug(LOG_MSG_MULTI, walletId, EventType.PROBLEM_REPORT, report);
//    }
//
//    public void handleSettings(String walletId, Settings settings) throws Exception {
//        log.debug(LOG_MSG_MULTI, walletId, EventType.SETTINGS, settings);
//    }

    public class WebSocketEvent {
        private final String theirWalletId;
        private final String topic;
        private final Object payload;
        
        WebSocketEvent(String theirWalletId, String topic, Object payload) {
            this.theirWalletId = theirWalletId;
            this.topic = topic;
            this.payload = payload;
        }

        public DefaultEventHandler getEventHandler() {
            return DefaultEventHandler.this;
        }
        
        public AriesClient createClient() throws IOException {
            return AriesClientFactory.createClient(getThisWallet());
        }
        
        public String getTopic() {
            return topic;
        }

        public Class<?> getEventType() {
            return payload.getClass();
        }

        public WalletRecord getThisWallet() {
            return DefaultEventHandler.this.thisWallet;
        }

        public String getThisWalletId() {
            return DefaultEventHandler.this.getThisWalletId();
        }

        public String getThisWalletName() {
            return DefaultEventHandler.this.getThisWalletName();
        }

        public String getTheirWalletId() {
            return theirWalletId;
        }

        public String getTheirWalletName() {
            return DefaultEventHandler.this.getWalletName(theirWalletId);
        }

        @SuppressWarnings("unchecked")
        public <T> T getPayload(Class<T> type) {
            return (T) payload;
        }
        
        public Object getPayload() {
            return payload;
        }
    }
    
}
