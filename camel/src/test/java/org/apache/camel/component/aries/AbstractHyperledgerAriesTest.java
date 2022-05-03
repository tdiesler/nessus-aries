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
import java.util.Arrays;

import org.apache.camel.test.junit5.CamelTestSupport;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.ledger.IndyLedgerRoles;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.config.GsonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.nessus.aries.util.AttachmentKey;
import io.nessus.aries.util.AttachmentSupport;
import io.nessus.aries.wallet.NessusWallet;
import io.nessus.aries.wallet.WalletBuilder;
import io.nessus.aries.wallet.WalletRegistry;

public abstract class AbstractHyperledgerAriesTest extends CamelTestSupport {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    static final String Government = "Government";
    static final String Faber = "Faber";
    static final String Acme = "Acme";
    static final String Thrift = "Thrift";
    static final String Alice = "Alice";
    
    static final Gson gson = GsonConfig.defaultConfig();
    
    private AttachmentContext attcontext = new AttachmentContext();
    
    public HyperledgerAriesComponent getComponent() {
        return context.getComponent("hyperledger-aries", HyperledgerAriesComponent.class);
    }

    public AttachmentContext getAttachmentContext() {
        return attcontext;
    }

    public void logSection(String title) {
        int len = 119 - title.length();
        char[] tail = new char[len];
        Arrays.fill(tail, '=');
        log.info("{} {}", title, String.valueOf(tail));
    }
    
    public NessusWallet onboardWallet(String walletName) throws IOException {
        return onboardWallet(walletName, null);
    }
    
    public NessusWallet onboardWallet(String walletName, IndyLedgerRoles role) throws IOException {

        logSection("Onboard " + walletName);
        
        WalletRegistry walletRegistry = getComponent().getWalletRegistry();
        
        NessusWallet walletRecord = new WalletBuilder(walletName)
                .walletRegistry(walletRegistry)
                .selfRegisterNym(role != null)
                .ledgerRole(role)
                .build();
        
        return walletRecord;
    }
    
    public AriesClient createClient(WalletRecord walletRecord) throws IOException {
        return getComponent().createClient(walletRecord);
    }
    
    // Attachment Support ===============================================================
    
    public static class AttachmentContext extends AttachmentSupport {

        public ConnectionRecord getConnection(String inviter, String invitee) {
            return getAttachment(inviter + invitee + "Connection", ConnectionRecord.class);
        }

        public <T> T getAttachment(String name, Class<T> type) {
            return getAttachment(new AttachmentKey<>(name, type));
        }
        
        public <T> T putAttachment(String name,  Class<T> type, T obj) {
            return putAttachment(new AttachmentKey<T>(name, type), obj);
        }
        
        public @SuppressWarnings("unchecked")
        <T> T putAttachment(String name,  T obj) {
            return putAttachment(new AttachmentKey<T>(name, (Class<T>) obj.getClass()), obj);
        }
    }
}
