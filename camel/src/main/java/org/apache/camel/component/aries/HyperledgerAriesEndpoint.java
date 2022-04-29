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

import java.io.IOException;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.aries.HyperledgerAriesConfiguration.ServiceId;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultEndpoint;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.IndyLedgerRoles;
import org.hyperledger.aries.api.multitenancy.RemoveWalletRequest;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.aries.AgentConfiguration;
import io.nessus.aries.AriesClientFactory;
import io.nessus.aries.util.AssertState;
import io.nessus.aries.wallet.WalletBuilder;

/**
 * Access market data and trade on Bitcoin and Altcoin exchanges.
 */
@UriEndpoint(firstVersion = "3.17.0", scheme = "hyperledger-aries", title = "Aries", syntax = "aries:walletName", producerOnly = true, category = { Category.BLOCKCHAIN })
public class HyperledgerAriesEndpoint extends DefaultEndpoint {

    static final Logger log = LoggerFactory.getLogger(HyperledgerAriesEndpoint.class);

    @UriParam
    private HyperledgerAriesConfiguration configuration;
    
    // The Wallet is associated with the Endpoint
    // It can be created on demand when enabled in the URI
    private WalletRecord walletRecord;

    public HyperledgerAriesEndpoint(String uri, HyperledgerAriesComponent component, HyperledgerAriesConfiguration configuration) {
        super(uri, component);
        this.configuration = configuration;
    }

    @Override
    public HyperledgerAriesComponent getComponent() {
        return (HyperledgerAriesComponent) super.getComponent();
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Producer createProducer() throws Exception {
        Producer producer = null;

        ServiceId service = getConfiguration().getService();
        if (ServiceId.wallet == service) {
            producer = new WalletServiceProducer(this);
        }

        AssertState.notNull(producer, "Unsupported service: " + service);
        return producer;
    }

    @Override
    protected void doShutdown() throws Exception {
        if (walletRecord != null && configuration.isWalletCreate() && configuration.isWalletRemove()) {
            String walletId = walletRecord.getWalletId();
            String walletName = configuration.getWalletName();
            log.info("{} Remove Wallet", walletName);
            WalletBuilder.walletRegistry.removeWallet(walletId);
            AgentConfiguration agentConfig = configuration.getAgentConfiguration();
            AriesClient baseClient = AriesClientFactory.baseClient(agentConfig);
            baseClient.multitenancyWalletRemove(walletId, RemoveWalletRequest.builder().build());
        }
    }

    public HyperledgerAriesConfiguration getConfiguration() {
        return configuration;
    }

    public AriesClient createClient() throws IOException {
        AgentConfiguration agentConfig = configuration.getAgentConfiguration();
        String walletName = configuration.getWalletName();
        IndyLedgerRoles walletRole = configuration.getWalletRole();
        if (walletRecord == null && configuration.isWalletCreate()) {
            walletRecord = new WalletBuilder(walletName)
                    .agentConfig(agentConfig)
                    .ledgerRole(walletRole)
                    .selfRegisterNym()
                    .build();
        }
        AssertState.notNull(walletRecord, "No WalletRecord for: " + walletName);
        AssertState.isEqual(walletName, walletRecord.getSettings().getWalletName());
        return AriesClientFactory.createClient(walletRecord, agentConfig);
    }
}
