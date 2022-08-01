package io.nessus.aries.websocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hyperledger.aries.BaseClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.discover_features.DiscoverFeatureEvent;
import org.hyperledger.aries.api.endorser.EndorseTransactionRecord;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueIndyCredentialEvent;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueLDCredentialEvent;
import org.hyperledger.aries.api.message.BasicMessage;
import org.hyperledger.aries.api.message.ProblemReport;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof_v2.V20PresExRecord;
import org.hyperledger.aries.api.revocation.RevocationEvent;
import org.hyperledger.aries.api.revocation.RevocationNotificationEvent;
import org.hyperledger.aries.api.revocation.RevocationNotificationEventV2;
import org.hyperledger.aries.api.revocation.RevocationRegistryState;
import org.hyperledger.aries.api.settings.Settings;
import org.hyperledger.aries.api.trustping.PingEvent;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.aries.webhook.EventParser;
import org.hyperledger.aries.webhook.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import io.nessus.aries.util.AssertState;
import io.nessus.aries.util.SafeConsumer;
import io.nessus.aries.util.ThreadUtils;
import io.nessus.aries.wallet.WalletRegistry;
import okhttp3.Response;
import okhttp3.WebSocket;

/**
 * An abstract WebSocketListener that gives access to the current state of the connection
 * as well as the stream of events seen by this listener.
 * 
 * By default, incomming events are simply logged and there is no event recording. An extension
 * of this WebSocketListener would implement the various `handleFoo` methods and process events
 * as needed by the application.
 * 
 * This class can also start/stop recording of incomming events by event type. 
 * These recorded events can later be retrieved by the application.
 *  
 * Note, there is currently no resource limit on the volume of recorded events. This must 
 * be taken care of by the application doing the recording.
 */
public class WebSocketListener extends okhttp3.WebSocketListener {

    static final Logger log = LoggerFactory.getLogger(WebSocketListener.class);

    private static final Gson gson = GsonConfig.defaultConfig();
    private static final Gson pretty = GsonConfig.prettyPrinter();
    
    public enum WebSocketState {
        NEW, OPEN, CLOSING, CLOSED
    }
    private WebSocketState state = WebSocketState.NEW;
    
    private final Map<EventType, List<WebSocketEvent>> recordedEvents = new HashMap<>();
    private final Lock accessLock = new ReentrantLock();
    private final EventParser parser = new EventParser();
    private final WalletRegistry walletRegistry;
    private final List<String> walletIdFilter;
    private final String label;

    public WebSocketListener(String label, WalletRegistry walletRegistry, List<String> walletIdFilter) {
		this.label = label;
		this.walletRegistry = walletRegistry;
		// [TODO] It should not be necessary to filter events targeted to other wallets
		this.walletIdFilter = walletIdFilter != null ? new ArrayList<>(walletIdFilter) : null;
	}

    public WebSocketState getWebSocketState() {
        return state;
    }

	@Override
    public void onOpen(WebSocket webSocket, Response response) {
        log.info("{}: WebSocket Open: {}", label, response);
        state = WebSocketState.OPEN;
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        log.info("{}: WebSocket Closing: {} {}", label, code, reason);
        state = WebSocketState.CLOSING;
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        log.info("{}: WebSocket Closed: {} {}", label, code, reason);
    	accessLock.lock();
    	try {
    		recordedEvents.clear();
            state = WebSocketState.CLOSED;
    	} finally {
        	accessLock.unlock();
    	}
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable th, Response response) {
        String message = response != null ? response.message() : th.getMessage();
        if (!"Socket closed".equals(message))
            log.error(String.format("[%s] Failure: %s", label, message), th);
    }
    @Override
    public void onMessage(WebSocket webSocket, String message) {
        log.trace("{} Event: {}", label, message);
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String walletId = json.has("wallet_id") ? json.get("wallet_id").getAsString() : null;
            String payload = json.has("payload") ? json.get("payload").toString() : BaseClient.EMPTY_JSON;
            String topic = json.get("topic").getAsString();

            // drop ws ping messages, not to be confused with aca-py ping message
            // https://datatracker.ietf.org/doc/html/rfc6455#section-5.5.2
            if (notWsPing(topic, payload) && isForWalletId(walletId)) {
                handleEvent(walletId, topic, payload);
                log.debug("{}", pretty.toJson(json));
            }
        } catch (JsonSyntaxException ex) {
            log.error("JsonSyntaxException", ex);
        }
    }

    private boolean notWsPing(String topic, String payload) {
        return !(EventType.PING.topicEquals(topic) && BaseClient.EMPTY_JSON.equals(payload));
    }

    private boolean isForWalletId(String walletId) {
        return walletIdFilter == null || walletIdFilter.contains(walletId);
    }
    
    private void handleEvent(String walletId, String topic, String payload) {
        try {
            Object value;
            SafeConsumer<WebSocketEvent> consumer;
            if (EventType.CONNECTIONS.topicEquals(topic)) {
                value = parser.parseValueSave(payload, ConnectionRecord.class).orElseThrow();
                consumer = ev -> handleConnection(ev);
            } else if (EventType.PRESENT_PROOF.topicEquals(topic)) {
                value = parser.parseValueSave(payload, PresentationExchangeRecord.class).orElseThrow();
                consumer = ev -> handlePresentProofV1(ev);
            } else if (EventType.PRESENT_PROOF_V2.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V20PresExRecord.class).orElseThrow();
                consumer = ev -> handlePresentProofV2(ev);
            } else if (EventType.ISSUE_CREDENTIAL.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V1CredentialExchange.class).orElseThrow();
                consumer = ev -> handleIssueCredentialV1(ev);
            } else if (EventType.ISSUE_CREDENTIAL_V2.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V20CredExRecord.class).orElseThrow();
                consumer = ev -> handleIssueCredentialV2(ev);
            } else if (EventType.ISSUE_CREDENTIAL_V2_INDY.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V2IssueIndyCredentialEvent.class).orElseThrow();
                consumer = ev -> handleIssueCredentialV2Indy(ev);
            } else if (EventType.ISSUE_CREDENTIAL_V2_LD_PROOF.topicEquals(topic)) {
                value = parser.parseValueSave(payload, V2IssueLDCredentialEvent.class).orElseThrow();
                consumer = ev -> handleIssueCredentialV2LD(ev);
            } else if (EventType.BASIC_MESSAGES.topicEquals(topic)) {
                value = parser.parseValueSave(payload, BasicMessage.class).orElseThrow();
                consumer = ev -> handleBasicMessage(ev);
            } else if (EventType.PING.topicEquals(topic)) {
                value = parser.parseValueSave(payload, PingEvent.class).orElseThrow();
                consumer = ev -> handleTrustPing(ev);
            } else if (EventType.ISSUER_CRED_REV.topicEquals(topic)) {
                value = parser.parseValueSave(payload, RevocationEvent.class).orElseThrow();
                consumer = ev -> handleIssuerRevocation(ev);
            } else if (EventType.ENDORSE_TRANSACTION.topicEquals(topic)) {
                value = parser.parseValueSave(payload, EndorseTransactionRecord.class).orElseThrow();
                consumer = ev -> handleEndorseTransaction(ev);
            } else if (EventType.PROBLEM_REPORT.topicEquals(topic)) {
                value = parser.parseValueSave(payload, ProblemReport.class).orElseThrow();
                consumer = ev -> handleProblemReport(ev);
            } else if (EventType.DISCOVER_FEATURE.topicEquals(topic)) {
                value = parser.parseValueSave(payload, DiscoverFeatureEvent.class).orElseThrow();
                consumer = ev -> handleDiscoverFeature(ev);
            } else if (EventType.REVOCATION_NOTIFICATION.topicEquals(topic)) {
                value = parser.parseValueSave(payload, RevocationNotificationEvent.class).orElseThrow();
                consumer = ev -> handleRevocationNotificationV1(ev);
            } else if (EventType.REVOCATION_NOTIFICATION_V2.topicEquals(topic)) {
                value = parser.parseValueSave(payload, RevocationNotificationEventV2.class).orElseThrow();
                consumer = ev -> handleRevocationNotificationV2(ev);
            } else if (EventType.SETTINGS.topicEquals(topic)) {
                value = parser.parseValueSave(payload, Settings.class).orElseThrow();
                consumer = ev -> handleSettings(ev);
            } else {
                log.warn("Unsupported event topic: {}", topic);
                return;
            }
            
            WebSocketEvent ev = recordEvent(new WebSocketEvent(walletId, topic, value));
			consumer.accept(ev);
            
        } catch (Throwable e) {
            log.error("Error in webhook event handler:", e);
        }
    }
    
	protected BasicMessage handleBasicMessage(WebSocketEvent ev) {
		BasicMessage item = ev.getPayload(BasicMessage.class);
        log.info("{}: [@{}] {} {} {}", label, ev.getWalletName(), item); 
        return item;
	}

    protected ConnectionRecord handleConnection(WebSocketEvent ev) throws Exception {
        ConnectionRecord item = ev.getPayload(ConnectionRecord.class);
        log.info("{}: [@{}] {} {} {}", label, ev.getWalletName(), item.getTheirRole(), item.getState(), item);
        return item;
    }

    protected DiscoverFeatureEvent handleDiscoverFeature(WebSocketEvent ev) {
		DiscoverFeatureEvent item = ev.getPayload(DiscoverFeatureEvent.class);
        log.info("{}: [@{}] {} {} {}", label, ev.getWalletName(), item); 
        return item;
	}

    protected EndorseTransactionRecord handleEndorseTransaction(WebSocketEvent ev) {
		EndorseTransactionRecord item = ev.getPayload(EndorseTransactionRecord.class);
        log.info("{}: [@{}] {} {} {}", label, ev.getWalletName(), item); 
        return item;
	}

    protected V1CredentialExchange handleIssueCredentialV1(WebSocketEvent ev) throws Exception {
        V1CredentialExchange item = ev.getPayload(V1CredentialExchange.class);
        log.info("{}: [@{}] {} {} {}", label, ev.getWalletName(), item.getRole(), item.getState(), item);
        return item;
    }
    
    protected V20CredExRecord handleIssueCredentialV2(WebSocketEvent ev) {
    	V20CredExRecord item = ev.getPayload(V20CredExRecord.class);
        log.info("{}: [@{}] {} {} {}", label, ev.getWalletName(), item); 
        return item;
	}

    protected V2IssueIndyCredentialEvent handleIssueCredentialV2Indy(WebSocketEvent ev) {
		V2IssueIndyCredentialEvent item = ev.getPayload(V2IssueIndyCredentialEvent.class);
        log.info("{}: [@{}] {} {} {}", label, ev.getWalletName(), item); 
        return item;
	}

    protected V2IssueLDCredentialEvent handleIssueCredentialV2LD(WebSocketEvent ev) {
		V2IssueLDCredentialEvent item = ev.getPayload(V2IssueLDCredentialEvent.class);
        log.info("{}: [@{}] {} {} {}", label, ev.getWalletName(), item); 
        return item;
	}

    protected RevocationEvent handleIssuerRevocation(WebSocketEvent ev) throws Exception {
        RevocationEvent item = ev.getPayload(RevocationEvent.class);
        log.info("{}: [@{}] {} {}", label, ev.getWalletName(), item.getState(), item);
        return item;
    }

    protected PresentationExchangeRecord handlePresentProofV1(WebSocketEvent ev) throws Exception {
        PresentationExchangeRecord item = ev.getPayload(PresentationExchangeRecord.class);
        log.info("{}: [@{}] {} {} {} {} {}", label, ev.getWalletName(), item.getRole(), item.getState(), item); 
        return item;
    }

    protected V20PresExRecord handlePresentProofV2(WebSocketEvent ev) {
		V20PresExRecord item = ev.getPayload(V20PresExRecord.class);
        log.info("{}: [@{}] {} {} {}", label, ev.getWalletName(), item); 
        return item;
	}

    protected ProblemReport handleProblemReport(WebSocketEvent ev) {
		ProblemReport item = ev.getPayload(ProblemReport.class);
        log.info("{}: [@{}] {} {} {}", label, ev.getWalletName(), item); 
        return item;
	}

    protected RevocationNotificationEvent handleRevocationNotificationV1(WebSocketEvent ev) {
		RevocationNotificationEvent item = ev.getPayload(RevocationNotificationEvent.class);
        log.info("{}: [@{}] {} {} {}", label, ev.getWalletName(), item); 
        return item;
	}

    protected RevocationNotificationEventV2 handleRevocationNotificationV2(WebSocketEvent ev) {
    	RevocationNotificationEventV2 item = ev.getPayload(RevocationNotificationEventV2.class);
        log.info("{}: [@{}] {} {} {}", label, ev.getWalletName(), item); 
        return item;
	}

	protected Settings handleSettings(WebSocketEvent ev) throws Exception {
        Settings item = ev.getPayload(Settings.class);
		log.info("{}: {}", ev.getWalletName(), item);
        return item;
    }

    protected PingEvent handleTrustPing(WebSocketEvent ev) {
		PingEvent item = ev.getPayload(PingEvent.class);
        log.info("{}: [@{}] {} {} {}", label, ev.getWalletName(), item); 
        return item;
	}

    public boolean isRecording(EventType evtype) {
    	accessLock.lock();
    	try {
        	return recordedEvents.containsKey(evtype);
    	} finally {
        	accessLock.unlock();
    	}
    }
    
	public void restartRecording(EventType... evtypes) {
		AssertState.notNull(evtypes, "Not evtypes");
    	accessLock.lock();
    	try {
    		for (EventType evt : evtypes) {
    			stopRecording(evt);
    			startRecording(evt);
    		}
    	} finally {
        	accessLock.unlock();
    	}
	}
	
	public void startRecording(EventType... evtypes) {
		AssertState.notNull(evtypes, "Not evtypes");
    	accessLock.lock();
    	try {
    		for (EventType evt : evtypes) {
        		if (!isRecording(evt)) {
        			recordedEvents.put(evt, new ArrayList<WebSocketEvent>());
        		}
    		}
    	} finally {
        	accessLock.unlock();
    	}
	}
	
	public void stopRecording(EventType... evtypes) {
    	accessLock.lock();
    	try {
    		for (EventType evt : evtypes) {
    			recordedEvents.remove(evt);
    		}
    	} finally {
        	accessLock.unlock();
    	}
	}
	
	private WebSocketEvent recordEvent(WebSocketEvent ev) {
		EventType evtype = EventType.fromTopic(ev.topic).get();
    	accessLock.lock();
    	try {
    		if (isRecording(evtype)) {
        		recordedEvents.get(evtype).add(ev);
    		}
    	} finally {
        	accessLock.unlock();
    	}
    	return ev;
	}
	
	public Stream<BasicMessage> awaitBasicMessage(Predicate<BasicMessage> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.BASIC_MESSAGES, BasicMessage.class, predicate, timeout, unit);
	}
	
	public Stream<ConnectionRecord> awaitConnection(Predicate<ConnectionRecord> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.CONNECTIONS, ConnectionRecord.class, predicate, timeout, unit);
	}
	
	public Stream<DiscoverFeatureEvent> awaitDiscoveredFeature(Predicate<DiscoverFeatureEvent> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.DISCOVER_FEATURE, DiscoverFeatureEvent.class, predicate, timeout, unit);
	}
	
	public Stream<EndorseTransactionRecord> awaitEndorseTransaction(Predicate<EndorseTransactionRecord> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.ENDORSE_TRANSACTION, EndorseTransactionRecord.class, predicate, timeout, unit);
	}
	
	public Stream<V1CredentialExchange> awaitIssueCredentialV1(Predicate<V1CredentialExchange> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.ISSUE_CREDENTIAL, V1CredentialExchange.class, predicate, timeout, unit);
	}
	
	public Stream<V20CredExRecord> awaitIssueCredentialV2(Predicate<V20CredExRecord> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.ISSUE_CREDENTIAL_V2, V20CredExRecord.class, predicate, timeout, unit);
	}
	
	public Stream<V2IssueIndyCredentialEvent> awaitIssueCredentialV2Indy(Predicate<V2IssueIndyCredentialEvent> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.ISSUE_CREDENTIAL_V2_INDY, V2IssueIndyCredentialEvent.class, predicate, timeout, unit);
	}
	
	public Stream<V2IssueLDCredentialEvent> awaitIssueCredentialV2LD(Predicate<V2IssueLDCredentialEvent> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.ISSUE_CREDENTIAL_V2_LD_PROOF, V2IssueLDCredentialEvent.class, predicate, timeout, unit);
	}
	
	public Stream<RevocationEvent> awaitIssuerRevocation(Predicate<RevocationEvent> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.ISSUER_CRED_REV, RevocationEvent.class, predicate, timeout, unit);
	}
	
	public Stream<PresentationExchangeRecord> awaitPresentProofV1(Predicate<PresentationExchangeRecord> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.PRESENT_PROOF, PresentationExchangeRecord.class, predicate, timeout, unit);
	}
	
	public Stream<V20PresExRecord> awaitPresentProofV2(Predicate<V20PresExRecord> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.PRESENT_PROOF_V2, V20PresExRecord.class, predicate, timeout, unit);
	}
	
	public Stream<ProblemReport> awaitProblemReport(Predicate<ProblemReport> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.PROBLEM_REPORT, ProblemReport.class, predicate, timeout, unit);
	}
	
	public Stream<RevocationNotificationEvent> awaitRevocationNotificationV1(Predicate<RevocationNotificationEvent> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.REVOCATION_NOTIFICATION, RevocationNotificationEvent.class, predicate, timeout, unit);
	}
	
	public Stream<RevocationNotificationEventV2> awaitRevocationNotificationV2(Predicate<RevocationNotificationEventV2> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.REVOCATION_NOTIFICATION_V2, RevocationNotificationEventV2.class, predicate, timeout, unit);
	}
	
	public Stream<RevocationRegistryState> awaitRevocationRegistry(Predicate<RevocationRegistryState> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.REVOCATION_REGISTRY, RevocationRegistryState.class, predicate, timeout, unit);
	}
	
	public Stream<Settings> awaitSettings(Predicate<Settings> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.SETTINGS, Settings.class, predicate, timeout, unit);
	}
	
	public Stream<PingEvent> awaitTrustPing(Predicate<PingEvent> predicate, long timeout, TimeUnit unit) {
		return getPayloadStream(EventType.PING, PingEvent.class, predicate, timeout, unit);
	}

	private <T> Stream<T> getPayloadStream(EventType evtype, Class<T> payloadType, Predicate<T> predicate, long timeout, TimeUnit unit) {
		List<T> result = Collections.emptyList();
		if (isRecording(evtype)) {
			long now = System.currentTimeMillis();
			long end = timeout = now + unit.toMillis(timeout);
			while (result.isEmpty() && now < end) {
		    	accessLock.lock();
		    	try {
        			result = recordedEvents.get(evtype).stream()
        					.map(ev -> ev.getPayload(payloadType))
        					.filter(ev -> predicate.test(ev))
        					.collect(Collectors.toList());
		    	} finally {
		        	accessLock.unlock();
		    	}
    			ThreadUtils.sleepWell(200);
    			now = System.currentTimeMillis();
			}
		}
		return result.stream();
	}
	
    public class WebSocketEvent {
        private final String topic;
        private final String walletId;
        private final Object payload;
        
        WebSocketEvent(String walletId, String topic, Object payload) {
            this.walletId = walletId;
            this.topic = topic;
            this.payload = payload;
        }

        public WebSocketListener getWebSocketListener() {
            return WebSocketListener.this;
        }
        
        public String getTopic() {
            return topic;
        }

        public Class<?> getEventType() {
            return payload.getClass();
        }

        public String getWalletName() {
            return walletRegistry != null ? walletRegistry.getWalletName(walletId) : label;
        }

        @SuppressWarnings("unchecked")
        public <T> T getPayload(Class<T> type) {
            return (T) payload;
        }
    }
}
