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
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultEndpoint;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.aries.AgentConfiguration;
import io.nessus.aries.AriesClientFactory;
import io.nessus.aries.util.AssertArg;
import io.nessus.aries.util.AssertState;

/**
 * Access market data and trade on Bitcoin and Altcoin exchanges.
 */
@UriEndpoint(firstVersion = "3.17.0", scheme = "hyperledger-aries", title = "Aries", syntax = "aries:walletName", producerOnly = true, category = { Category.BLOCKCHAIN })
public class HyperledgerAriesEndpoint extends DefaultEndpoint {

    static final Logger log = LoggerFactory.getLogger(HyperledgerAriesEndpoint.class);

    @UriParam
    private HyperledgerAriesConfiguration configuration;
    
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
        return new HyperledgerAriesProducer(this);
    }

    public HyperledgerAriesConfiguration getConfiguration() {
        return configuration;
    }
    
    public String getWalletName() {
        return getConfiguration().getWallet();
    }
    
    public WalletRecord getWalletRecord() {
        return getComponent().getWalletByName(getWalletName());
    }
    
    public AriesClient baseClient() {
        AgentConfiguration agentConfig = getComponent().getAgentConfiguration();
        return AriesClientFactory.baseClient(agentConfig);
    }
    
    public AriesClient createClient() throws IOException {
        WalletRecord walletRecord = getWalletRecord();
        AssertState.notNull(walletRecord, "No WalletRecord for: " + getWalletRecord());
        return createClient(walletRecord);
    }

    public AriesClient createClient(WalletRecord walletRecord) throws IOException {
        AssertArg.notNull(walletRecord, "No WalletRecord");
        AgentConfiguration agentConfig = getComponent().getAgentConfiguration();
        return AriesClientFactory.createClient(walletRecord, agentConfig);
    }
}
