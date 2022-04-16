
package io.nessus.aries.test;

import static io.nessus.aries.common.WebSockets.createWebSocket;
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;
import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.TRUSTEE;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.did_exchange.DidExchangeCreateRequestFilter;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.junit.jupiter.api.Test;

import io.nessus.aries.common.Configuration;
import io.nessus.aries.common.WalletRegistry;
import io.nessus.aries.common.WebSocketEventHandler;
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
        WalletRecord govrnWallet = new WalletBuilder("Government")
                .ledgerRole(TRUSTEE).selfRegisterNym().build();

        WalletRecord faberWallet = new WalletBuilder("Faber")
                .registerNym(govrnWallet).ledgerRole(ENDORSER).build();
        
        AriesClient faber = createClient(faberWallet);
        AriesClient govern = createClient(govrnWallet);
        
        WebSocket governWebSocket = createWebSocket(govrnWallet, new WebSocketEventHandler() {
            
            @Override
            public void handleRaw(String walletId, String topic, String payload) {
                String sourceWallet = WalletRegistry.getWalletName(walletId);
                String myWalletName = govrnWallet.getSettings().getWalletName();
                log.info("{} Event {}: [@{}] {}", myWalletName, topic, sourceWallet, payload);
            }

            @Override
            public void handleConnection(String walletId, ConnectionRecord con) {
                String sourceWallet = WalletRegistry.getWalletName(walletId);
                String myWalletName = govrnWallet.getSettings().getWalletName();
                String myWalletId = govrnWallet.getWalletId();
                ConnectionState state = con.getState();
                log.info("{} Event: [@{}] [{}] {}", myWalletName, sourceWallet, state, con);
                
                if (myWalletId.equals(walletId) && ConnectionState.ACTIVE == state) {
                    log.info("{} CONNECTION ACTIVE", myWalletName);
                }
            }
        });
        
        WebSocket faberWebSocket = createWebSocket(faberWallet, new WebSocketEventHandler() {
            @Override
            public void handleRaw(String walletId, String topic, String payload) {
                String sourceWallet = WalletRegistry.getWalletName(walletId);
                String myWalletName = faberWallet.getSettings().getWalletName();
                log.info("{} Event {}: [@{}] {}", myWalletName, topic, sourceWallet, payload);
            }

            @Override
            public void handleConnection(String walletId, ConnectionRecord con) {
                String sourceWallet = WalletRegistry.getWallet(walletId).getSettings().getWalletName();
                String myWalletName = faberWallet.getSettings().getWalletName();
                String myWalletId = faberWallet.getWalletId();
                ConnectionState state = con.getState();
                log.info("{} Event: [@{}] [{}] {}", myWalletName, sourceWallet, state, con);
                
                if (myWalletId.equals(walletId) && ConnectionState.ACTIVE == state) {
                    log.info("{} CONNECTION ACTIVE", myWalletName);
                }
            }
        });
        
        try {
            DID governPublicDid = govern.walletDidPublic().get();
            
            ConnectionRecord connectionRecord = faber.didExchangeCreateRequest(DidExchangeCreateRequestFilter.builder()
                    .theirPublicDid(governPublicDid.getDid())
                    .myEndpoint(Configuration.ACAPY_USER_URL)
                    .myLabel("Faber/Government")
                    .build()).get();
            log.info("Faber: {}", connectionRecord);

            
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
            
            /* Government accepts connection request
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

            Thread.sleep(10000);

        } finally {
            faberWebSocket.close(1000, null);
            governWebSocket.close(1000, null);
            removeWallet(govrnWallet);
            removeWallet(faberWallet);
        }
    }
}
