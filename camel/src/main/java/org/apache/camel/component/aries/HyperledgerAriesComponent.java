/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.aries;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.multitenancy.RemoveWalletRequest;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.aries.AgentConfiguration;
import io.nessus.aries.AriesClientFactory;
import io.nessus.aries.wallet.WalletRegistry;

@Component("hyperledger-aries")
public class HyperledgerAriesComponent extends DefaultComponent {

    static final Logger log = LoggerFactory.getLogger(HyperledgerAriesComponent.class);

    private final WalletRegistry walletRegistry = new WalletRegistry();
    private AgentConfiguration agentConfig;
    
    @Metadata(description = "Remove wallets from the Agent on shutdown")
    private boolean removeWalletsOnShutdown;
    
    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        HyperledgerAriesConfiguration configuration = new HyperledgerAriesConfiguration();
        configuration.setWallet(remaining);

        HyperledgerAriesEndpoint endpoint = new HyperledgerAriesEndpoint(uri, this, configuration);
        setProperties(endpoint, parameters);

        // after configuring endpoint then get or create xchange instance
        // endpoint.setXchange(null);

        return endpoint;
    }

    @Override
    protected void doShutdown() throws Exception {
        if (removeWalletsOnShutdown) {
            AgentConfiguration agentConfig = getAgentConfiguration();
            AriesClient baseClient = AriesClientFactory.baseClient(agentConfig);
            for (WalletRecord wallet : walletRegistry.getWallets()) {
                log.info("Remove Wallet: {}", wallet.getSettings().getWalletName());
                baseClient.multitenancyWalletRemove(wallet.getWalletId(), RemoveWalletRequest.builder()
                        .walletKey(wallet.getToken())
                        .build());
            }
        }
    }

    public boolean isRemoveWalletsOnShutdown() {
        return removeWalletsOnShutdown;
    }

    public void setRemoveWalletsOnShutdown(boolean removeWalletsOnShutdown) {
        this.removeWalletsOnShutdown = removeWalletsOnShutdown;
    }

    public AgentConfiguration getAgentConfiguration() {
        if (agentConfig == null) {
            agentConfig = AgentConfiguration.defaultConfiguration();
        }
        return agentConfig;
    }
    
    public WalletRegistry getWalletRegistry() {
        return walletRegistry;
    }

    public void addWallet(WalletRecord wallet) {
        walletRegistry.putWallet(wallet);
    }
    
    public WalletRecord getWalletByName(String walletName) {
        return walletRegistry.getWalletByName(walletName);
    }
}
