
package io.nessus.aries.test;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.did_exchange.DidExchangeCreateRequestFilter;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.webhook.WebSocketEventHandler;
import org.junit.jupiter.api.Test;

import okhttp3.WebSocket;

/**
 * Test RFC 0023: DID Exchange Protocol 1.0 with multitenant wallets
 * 
 * https://github.com/hyperledger/aries-rfcs/tree/main/features/0023-did-exchange
 */
public class DidExchangeTest extends AbstractAriesTest {

    @Test
    void testDidExchange() throws Exception {

        // Create multitenant wallets
        WalletRecord govenWallet = createWallet("Government").role(ENDORSER).build();
        WalletRecord faberWallet = createWallet("Faber").role(ENDORSER).build();
        
        WebSocket governWebSocket = createWebSocket(govenWallet, new WebSocketEventHandler());
        WebSocket faberWebSocket = createWebSocket(faberWallet, new WebSocketEventHandler());
        
        try {
            AriesClient govern = useWallet(govenWallet);
            AriesClient faber = useWallet(faberWallet);
            
            DID governPublicDid = govern.walletDidPublic().get();
            
            ConnectionRecord faberRecord = faber.didExchangeCreateRequest(DidExchangeCreateRequestFilter.builder()
                    .theirPublicDid(governPublicDid.getDid())
                    .myEndpoint(ACAPY_USER_URL)
                    .myLabel("Faber/Government")
                    .build()).get();
            log.info("Faber: {}", faberRecord);

            assertConnectionState(faberWallet, ConnectionState.REQUEST);
            
            /* Faber receives the request
             * 
             * Fails with DIDXRequest.DIDXThread not initialised ???
             * 
            faber.didExchangeReceiveRequest(DIDXRequest.builder()
                    .id(connectionRecord.getRequestId())
                    .label("govrn/Faber") // label required, but not part of govrn's record
                    //.did("govrn's DID ???")
                    //.type("???")
                    //.didDocAttach(???)
                    .build(), DidExchangeReceiveRequestFilter.builder()
                        .autoAccept(true)
                        .build()).get();
            */
            
            /* govrn accepts connection request
             * 
             * Fails with code=404 message=Record not found
            govrn.didExchangeAcceptRequest(govrnConnectionId, DidExchangeAcceptRequestFilter.builder().build()).get();
             */
            
            /* Faber accepts the request
             *
             * Fails because Faber does not (yet) have a connection id
             faber.didExchangeAcceptRequest(faberConnectionId, null);
             */
            
//            awaitConnectionState(govrnWallet, ConnectionState.ACTIVE);
            
            awaitConnectionState(faberWallet, ConnectionState.ACTIVE);

        } finally {
            faberWebSocket.close(1000, null);
            governWebSocket.close(1000, null);
            removeWallet(govenWallet);
            removeWallet(faberWallet);
        }
    }
}

/*

It seems that the ping event is not what we expect

```
2022-04-11 17:05:50 INFO  [io.nessus.aries.test.did_exchange.DidExchangeTest] - WebSocket Message: {"topic": "ping", "authenticated": false}
2022-04-11 17:05:50 ERROR [org.hyperledger.aries.webhook.EventHandler] - Error in webhook event handler:
java.lang.NullPointerException: json is marked non-null but is null
    at org.hyperledger.aries.webhook.EventParser.parseValueSave(EventParser.java:41) ~[aries-client-python-0.7.23.jar:?]
    at org.hyperledger.aries.webhook.EventHandler.handleEvent(EventHandler.java:54) [aries-client-python-0.7.23.jar:?]
    at io.nessus.aries.test.did_exchange.DidExchangeTest$1.onMessage(DidExchangeTest.java:127) [test-classes/:?]
```

I'm also receiving a `settings` event that isn't handled at all

```
2022-04-11 17:05:15 INFO  [io.nessus.aries.test.did_exchange.DidExchangeTest] - WebSocket Message: {"topic": "settings", "payload": {"authenticated": false, "label": "Aries Cloud Agent", "endpoint": "http://localhost:8030", "no_receive_invites": false, "help_link": null}}
2022-04-11 17:05:15 ERROR [io.nessus.aries.test.did_exchange.DidExchangeTest] - JsonSyntaxException
com.google.gson.JsonSyntaxException: java.lang.IllegalStateException: Expected a string but was BEGIN_OBJECT at line 1 column 35 path $.payload
    at com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$Adapter.read(ReflectiveTypeAdapterFactory.java:226) ~[gson-2.8.6.jar:?]
    at com.google.gson.Gson.fromJson(Gson.java:932) ~[gson-2.8.6.jar:?]
    at com.google.gson.Gson.fromJson(Gson.java:897) ~[gson-2.8.6.jar:?]
    at com.google.gson.Gson.fromJson(Gson.java:846) ~[gson-2.8.6.jar:?]
    at com.google.gson.Gson.fromJson(Gson.java:817) ~[gson-2.8.6.jar:?]
    at io.nessus.aries.test.did_exchange.DidExchangeTest$1.onMessage(DidExchangeTest.java:125) [test-classes/:?]
```

Code is [here](https://github.com/tdiesler/nessus-aries/blob/next/itests/smoke/src/test/java/io/nessus/aries/test/did_exchange/DidExchangeTest.java#L51)

BTW, thanks for your continuous feedback. Please stay with me, at the end I'll do a step-by-step write up for the Alice-Faber-Acme walkthrough using this API. Folks coming after this, should have an easier on-ramp.

*/