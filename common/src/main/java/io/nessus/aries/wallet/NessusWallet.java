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
import java.util.Collections;
import java.util.List;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.multitenancy.RemoveWalletRequest;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.config.GsonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.nessus.aries.AgentConfiguration;
import io.nessus.aries.AriesClientFactory;
import io.nessus.aries.util.AssertState;
import io.nessus.aries.util.ThreadUtils;
import io.nessus.aries.websocket.WebSocketClient;
import io.nessus.aries.websocket.WebSocketListener;

/**
 * A NessusWallet gives acces to wallet information as known by the agent.
 * 
 * It also serves as a factory for the JSON-RPC Client as well as the WebSocket Client  
 * 
 */
public class NessusWallet extends WalletRecord implements AutoCloseable {

    static final Logger log = LoggerFactory.getLogger(WalletRecord.class);
    
    static final Gson gson = GsonConfig.defaultConfig();
    
    private transient WalletRegistry walletRegistry;
    private transient WebSocketClient wsclient;
    private transient AriesClient rpclient;
    private transient DID publicDid;
    
    public static NessusWallet build(WalletRecord wr) {
        String json = gson.toJson(wr);
        NessusWallet wallet = gson.fromJson(json, NessusWallet.class);
        return wallet;
    }
    
    public NessusWallet withWalletRegistry(WalletRegistry walletRegistry) {
        this.walletRegistry = walletRegistry;
        return this;
    }
    
    public String getWalletName() {
        return getSettings().getWalletName();
    }

    public DID getPublicDid() {
        return publicDid;
    }

    public void setPublicDid(DID publicDid) {
        this.publicDid = publicDid;
    }

    public WalletRegistry getWalletRegistry() {
        return walletRegistry;
    }

    public AriesClient getClient() {
        return rpclient;
    }
    
    public WebSocketClient getWebSocketClient() {
        return wsclient;
    }
    
    public AriesClient createClient() {
        return createClient(AgentConfiguration.defaultConfiguration());
    }
    
    public AriesClient createClient(AgentConfiguration config) {
        return rpclient = AriesClientFactory.createClient(config, this);
    }
    
    public WebSocketClient createWebSocketClient() {
        return createWebSocketClient(AgentConfiguration.defaultConfiguration(), null);
    }
    
    public WebSocketClient createWebSocketClient(AgentConfiguration config) {
        return createWebSocketClient(config, null);
    }
    
    public WebSocketClient createWebSocketClient(AgentConfiguration config, WebSocketListener wslistener) {
    	AssertState.isNull(wsclient, "WebSocket client already created");
    	if (wslistener == null) {
    		List<String> walletIdFilter = Collections.singletonList(getWalletId());
    		wslistener = new WebSocketListener(getWalletName(), walletRegistry, walletIdFilter);
    	}
    	wsclient = new WebSocketClient(config, this);
    	wsclient.openWebSocket(wslistener);
        return wsclient;
    }
    
    @Override
    public void close() throws IOException {
        closeWebSocket();
    }

    public synchronized void closeWebSocket() {
        if (wsclient != null) {
        	wsclient.close();
            wsclient = null;
        }
    }
    
    public void closeAndRemove() throws IOException {
        
        log.info("Remove Wallet: {}", getWalletName());
        
        if (walletRegistry != null)
            walletRegistry.removeWallet(getWalletId());
            
        AriesClient adminClient = AriesClientFactory.adminClient();
        adminClient.multitenancyWalletRemove(getWalletId(), RemoveWalletRequest.builder()
                .walletKey(getToken())
                .build());

        // Wait for the wallet to get removed 
        ThreadUtils.sleepWell(500); 
        while (!adminClient.multitenancyWallets(getWalletName()).get().isEmpty()) {
            ThreadUtils.sleepWell(500); 
        }
    }
}
