package io.nessus.aries.websocket;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
import org.hyperledger.aries.webhook.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.aries.AgentConfiguration;
import io.nessus.aries.util.AssertArg;
import io.nessus.aries.util.AssertState;
import io.nessus.aries.wallet.NessusWallet;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

/**
 * Creates and maintains a WebSocket connection (optionally) on behalf of a given wallet.
 * 
 * This client is associated with a given WebSocketListener, which can be used to record 
 * WebSocket events by event type.
 */
public class WebSocketClient {

    static final Logger log = LoggerFactory.getLogger(WebSocketClient.class);

    private final AgentConfiguration agentConfig;
    private final NessusWallet wallet;
    private WebSocketListener wslistener;
    private WebSocket webSocket;
    
    /**
     *  
     */
    public WebSocketClient(AgentConfiguration agentConfig, NessusWallet wallet) {
    	AssertArg.notNull(agentConfig, "No agentConfig");
    	this.agentConfig = agentConfig;
    	this.wallet = wallet;
	}

    public WebSocket getWebSocket() {
		return webSocket;
	}

	public WebSocketListener getWebSocketListener() {
		return wslistener;
	}

	public void openWebSocket(WebSocketListener listener) {
    	AssertArg.notNull(listener, "No listener");
        Request.Builder b = new Request.Builder();
        b.url(agentConfig.getWebSocketUrl());
        if (agentConfig.getApiKey() != null) {
            b.header("X-API-Key", agentConfig.getApiKey());
        }
        if (wallet != null && wallet.getToken() != null) {
            b.header("Authorization", "Bearer " + wallet.getToken());
        }
        Request request = b.build();
        OkHttpClient httpClient = new OkHttpClient();
        webSocket = httpClient.newWebSocket(request, listener);
        wslistener = listener;
    }

	public void close() {
		if (webSocket != null) {
			webSocket.close(1001, null);
			webSocket = null;
		}
	}

	public WebSocketClient startRecording(EventType... evtypes) {
		AssertState.notNull(wslistener, "Not connected");
		wslistener.startRecording(evtypes);
		return this;
	}

	public WebSocketClient retartRecording(EventType... evtypes) {
		AssertState.notNull(wslistener, "Not connected");
		wslistener.startRecording(evtypes);
		return this;
	}

	public WebSocketClient stopRecording(EventType... evtypes) {
		AssertState.notNull(wslistener, "Not connected");
		wslistener.stopRecording(evtypes);
		return this;
	}

	public Stream<BasicMessage> awaitBasicMessage(Predicate<BasicMessage> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitBasicMessage(predicate, timeout, unit);
	}
	
	public Stream<ConnectionRecord> awaitConnection(Predicate<ConnectionRecord> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitConnection(predicate, timeout, unit);
	}

	public Stream<DiscoverFeatureEvent> awaitDiscoveredFeature(Predicate<DiscoverFeatureEvent> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitDiscoveredFeature(predicate, timeout, unit);
	}
	
	public Stream<EndorseTransactionRecord> awaitEndorseTransaction(Predicate<EndorseTransactionRecord> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitEndorseTransaction(predicate, timeout, unit);
	}
	
	public Stream<V1CredentialExchange> awaitIssueCredentialV1(Predicate<V1CredentialExchange> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitIssueCredentialV1(predicate, timeout, unit);
	}
	
	public Stream<V20CredExRecord> awaitIssueCredentialV2(Predicate<V20CredExRecord> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitIssueCredentialV2(predicate, timeout, unit);
	}
	
	public Stream<V2IssueIndyCredentialEvent> awaitIssueCredentialV2Indy(Predicate<V2IssueIndyCredentialEvent> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitIssueCredentialV2Indy(predicate, timeout, unit);
	}
	
	public Stream<V2IssueLDCredentialEvent> awaitIssueCredentialV2LD(Predicate<V2IssueLDCredentialEvent> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitIssueCredentialV2LD(predicate, timeout, unit);
	}
	
	public Stream<RevocationEvent> awaitIssuerRevocation(Predicate<RevocationEvent> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitIssuerRevocation(predicate, timeout, unit);
	}
	
	public Stream<PresentationExchangeRecord> awaitPresentProofV1(Predicate<PresentationExchangeRecord> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitPresentProofV1(predicate, timeout, unit);
	}
	
	public Stream<V20PresExRecord> awaitPresentProofV2(Predicate<V20PresExRecord> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitPresentProofV2(predicate, timeout, unit);
	}
	
	public Stream<ProblemReport> awaitProblemReport(Predicate<ProblemReport> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitProblemReport(predicate, timeout, unit);
	}
	
	public Stream<RevocationNotificationEvent> awaitRevocationNotificationV1(Predicate<RevocationNotificationEvent> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitRevocationNotificationV1(predicate, timeout, unit);
	}
	
	public Stream<RevocationNotificationEventV2> awaitRevocationNotificationV2(Predicate<RevocationNotificationEventV2> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitRevocationNotificationV2(predicate, timeout, unit);
	}
	
	public Stream<RevocationRegistryState> awaitRevocationRegistry(Predicate<RevocationRegistryState> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitRevocationRegistry(predicate, timeout, unit);
	}
	
	public Stream<Settings> awaitSettings(Predicate<Settings> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitSettings(predicate, timeout, unit);
	}
	
	public Stream<PingEvent> awaitTrustPing(Predicate<PingEvent> predicate, long timeout, TimeUnit unit) {
		AssertState.notNull(wslistener, "Not connected");
		return wslistener.awaitTrustPing(predicate, timeout, unit);
	}
}
