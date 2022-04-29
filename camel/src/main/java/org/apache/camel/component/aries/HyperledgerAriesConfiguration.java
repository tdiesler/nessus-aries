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

import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;
import org.apache.camel.spi.UriPath;
import org.hyperledger.aries.api.ledger.IndyLedgerRoles;

import io.nessus.aries.AgentConfiguration;

@UriParams
public class HyperledgerAriesConfiguration {

    private AgentConfiguration agentConfig;
    
    // Available service
    public enum ServiceId {
        wallet
    }

    // Available methods
    public enum MethodId {
        publicDid
    }

    @UriPath(description = "The wallet to connect to")
    @Metadata(required = true)
    private String walletName;
    @UriParam(description = "The service to call")
    @Metadata(required = true)
    private ServiceId service;
    @UriParam(description = "The method to execute")
    @Metadata(required = true)
    private MethodId method;
    @UriParam(description = "Enable wallet creation on-demand")
    private boolean walletCreate;
    @UriParam(description = "Enable wallet removal on shutdown")
    private boolean walletRemove;
    @UriParam(description = "The wallet wallet role when created on demand")
    private IndyLedgerRoles walletRole;

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String name) {
        this.walletName = name;
    }

    public ServiceId getService() {
        return service;
    }

    public void setService(ServiceId service) {
        this.service = service;
    }

    public MethodId getMethod() {
        return method;
    }

    public void setMethod(MethodId method) {
        this.method = method;
    }
    
    public boolean isWalletCreate() {
        return walletCreate;
    }

    public void setWalletCreate(boolean walletCreate) {
        this.walletCreate = walletCreate;
    }

    public boolean isWalletRemove() {
        return walletRemove;
    }

    public void setWalletRemove(boolean walletRemove) {
        this.walletRemove = walletRemove;
    }

    public IndyLedgerRoles getWalletRole() {
        return walletRole;
    }

    public void setWalletRole(IndyLedgerRoles walletRole) {
        this.walletRole = walletRole;
    }

    public AgentConfiguration getAgentConfiguration() {
        if (agentConfig == null) {
            agentConfig = AgentConfiguration.defaultConfiguration();
        }
        return agentConfig;
    }
}
