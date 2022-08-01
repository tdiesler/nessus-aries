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
package io.nessus.aries;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.multitenancy.WalletRecord;

public class AriesClientFactory {

    /**
     * Create a client for the admin wallet
     */
    public static AriesClient adminClient() {
        return createClient(AgentConfiguration.defaultConfiguration(), null);
    }
    
    /**
     * Create a client for the admin wallet
     */
    public static AriesClient adminClient(AgentConfiguration config) {
        return createClient(config, null);
    }
    
    /**
     * Create a client for a multitenant wallet
     */
    public static AriesClient createClient(WalletRecord wallet) {
        return createClient(AgentConfiguration.defaultConfiguration(), wallet);
    }
    
    /**
     * Create a client for a multitenant wallet
     */
    public static AriesClient createClient(AgentConfiguration config, WalletRecord wallet) {
        return AriesClient.builder()
                .url(config.getAdminUrl())
                .apiKey(config.getApiKey())
                .bearerToken(wallet != null ? wallet.getToken() : null)
                .build();
    }
    
}
